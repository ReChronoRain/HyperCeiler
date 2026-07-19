package io.github.lingqiqi5211.ezhooktool.xposed.common

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.util.Collections

internal class InvocationContext(private val chain: XposedInterface.Chain) {
    private val originalThisObject: Any? = chain.thisObject
    private val originalArgs: Array<Any?> = chain.args.toTypedArray()

    val executable: Executable = chain.executable
    var thisObject: Any? = originalThisObject
    var args: Array<Any?> = originalArgs.copyOf()
    var isAfterStage: Boolean = false
    var skipped: Boolean = false
    var result: Any? = null
    var throwable: Throwable? = null

    fun proceedOriginal() {
        if (skipped) return
        try {
            val receiver = thisObject
            result = if (receiver == null) {
                chain.proceed(args)
            } else {
                chain.proceedWith(receiver, args)
            }
            throwable = null
        } catch (t: Throwable) {
            result = null
            throwable = t
        }
    }

    fun fallbackToOriginal(): Any? {
        thisObject = originalThisObject
        args = originalArgs.copyOf()
        skipped = false
        result = null
        throwable = null
        proceedOriginal()
        return resultOrThrow()
    }
}

/**
 * 由 [HookChain] 在 stage callback 抛错时包装抛出，让 hooker 层能拿到 phase 信息打日志。
 *
 * 不会逃逸到使用者代码——`buildHooker` 会捕获并解包 [cause]。
 */
internal class HookStageException(
    val phase: String,
    cause: Throwable,
    private val fallback: () -> Any?,
) : RuntimeException(cause) {
    fun fallback(): Any? = fallback.invoke()
}

private fun InvocationContext.resultOrThrow(): Any? {
    throwable?.let { throw it }
    return result
}

internal fun interface ChainStage {
    fun intercept(context: InvocationContext, proceed: () -> Unit)
}

internal class HookChain(private val stages: List<ChainStage>) {
    fun invoke(chain: XposedInterface.Chain): Any? {
        val context = InvocationContext(chain)
        val beforeStages = stages.filterIsInstance<BeforeChainStage>()
        val aroundStages = stages.filter { it !is BeforeChainStage && it !is AfterChainStage }
        val afterStages = stages.filterIsInstance<AfterChainStage>()

        for (stage in beforeStages) {
            stage.intercept(context) {}
            if (context.skipped) {
                break
            }
        }

        fun proceed(index: Int) {
            if (index >= aroundStages.size) {
                context.proceedOriginal()
                return
            }
            aroundStages[index].intercept(context) { proceed(index + 1) }
        }

        if (!context.skipped) {
            proceed(0)
        }

        afterStages.asReversed().forEach { stage ->
            stage.intercept(context) {}
        }

        context.throwable?.let { throw it }
        return context.result
    }
}

internal class BeforeChainStage(
    private val callback: (HookParam) -> Unit,
) : ChainStage {
    override fun intercept(context: InvocationContext, proceed: () -> Unit) {
        context.isAfterStage = false
        try {
            callback(HookParam(context))
        } catch (t: Throwable) {
            throw HookStageException("before", t, context::fallbackToOriginal)
        }
        if (!context.skipped) proceed()
    }
}

internal class AfterChainStage(
    private val callback: (HookParam) -> Unit,
) : ChainStage {
    override fun intercept(context: InvocationContext, proceed: () -> Unit) {
        proceed()
        val savedResult = context.result
        val savedThrowable = context.throwable
        context.isAfterStage = true
        try {
            callback(HookParam(context))
        } catch (t: Throwable) {
            throw HookStageException("after", t) {
                savedThrowable?.let { throw it }
                savedResult
            }
        } finally {
            context.isAfterStage = false
        }
    }
}

internal class ReplaceChainStage(
    private val callback: (HookParam) -> Any?,
) : ChainStage {
    override fun intercept(context: InvocationContext, proceed: () -> Unit) {
        context.isAfterStage = false
        context.skipped = true
        context.result = try {
            callback(HookParam(context))
        } catch (t: Throwable) {
            throw HookStageException("replace", t, context::fallbackToOriginal)
        }
        context.throwable = null
    }
}

internal class InterceptChainStage(
    private val callback: (XposedInterface.Chain) -> Any?,
) : ChainStage {
    override fun intercept(context: InvocationContext, proceed: () -> Unit) {
        context.isAfterStage = false
        var proceedCount = 0
        val ownerThread = Thread.currentThread()
        var closed = false
        val chain = object : XposedInterface.Chain {
            private fun ensureUsable() {
                check(Thread.currentThread() === ownerThread) { "Chain cannot be shared across threads" }
                check(!closed) { "Chain cannot be reused after intercept returns" }
            }

            override fun getExecutable(): Executable {
                ensureUsable()
                return context.executable
            }

            override fun getThisObject(): Any? {
                ensureUsable()
                return context.thisObject
            }

            override fun getArgs(): List<Any?> {
                ensureUsable()
                return Collections.unmodifiableList(context.args.asList())
            }

            override fun getArg(index: Int): Any? {
                ensureUsable()
                return context.args[index]
            }

            override fun proceedWith(thisObject: Any): Any? {
                ensureUsable()
                return proceedWith(thisObject, context.args)
            }

            override fun proceedWith(thisObject: Any, args: Array<out Any?>): Any? {
                ensureUsable()
                val originalThisObject = context.thisObject
                context.thisObject = thisObject
                try {
                    return proceedInternal(Array(args.size) { args[it] })
                } finally {
                    context.thisObject = originalThisObject
                }
            }

            private fun proceedInternal(args: Array<Any?>): Any? {
                ensureUsable()
                proceedCount += 1
                context.args = args
                proceed()
                context.throwable?.let { throw it }
                return context.result
            }

            override fun proceed(args: Array<out Any?>): Any? {
                ensureUsable()
                return proceedInternal(Array(args.size) { args[it] })
            }

            override fun proceed(): Any? {
                ensureUsable()
                return proceedInternal(context.args)
            }
        }

        try {
            val result = try {
                callback(chain)
            } catch (t: Throwable) {
                if (t is HookStageException || context.throwable === t) throw t
                throw HookStageException("intercept", t) {
                    if (proceedCount == 0) context.fallbackToOriginal() else context.resultOrThrow()
                }
            }
            if (proceedCount == 0) {
                context.skipped = true
            }
            context.result = result
            context.throwable = null
        } finally {
            closed = true
        }
    }
}
