package io.github.kyuubiran.ezxhelper.xposed.common

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.util.Collections

internal class InvocationContext(private val chain: XposedInterface.Chain) {
    val executable: Executable = chain.executable
    var thisObject: Any? = chain.thisObject
    var args: Array<Any?> = chain.args.toTypedArray()
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
}

internal fun interface ChainStage {
    fun intercept(context: InvocationContext, proceed: () -> Unit)
}

internal class HookChain(private val stages: List<ChainStage>) {
    fun invoke(chain: XposedInterface.Chain): Any? {
        val context = InvocationContext(chain)
        val beforeStages = stages.filterIsInstance<BeforeChainStage>()
        // Keep replace/intercept stages in declaration order so replacement hooks
        // participate in the execution chain instead of falling through.
        val coreStages = stages.filterNot { it is BeforeChainStage || it is AfterChainStage }
        val afterStages = stages.filterIsInstance<AfterChainStage>()

        for (stage in beforeStages) {
            stage.intercept(context) {}
            if (context.skipped) {
                break
            }
        }

        fun proceed(index: Int) {
            if (index >= coreStages.size) {
                context.proceedOriginal()
                return
            }
            coreStages[index].intercept(context) { proceed(index + 1) }
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
        callback(HookParam(context))
        if (!context.skipped) proceed()
    }
}

internal class AfterChainStage(
    private val callback: (HookParam) -> Unit,
) : ChainStage {
    override fun intercept(context: InvocationContext, proceed: () -> Unit) {
        proceed()
        context.isAfterStage = true
        try {
            callback(HookParam(context))
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
        context.result = callback(HookParam(context))
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
            val result = callback(chain)
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
