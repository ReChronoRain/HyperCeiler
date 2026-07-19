package io.github.lingqiqi5211.ezhooktool.xposed.dsl

import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.core.EzReflect
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.common.AfterChainStage
import io.github.lingqiqi5211.ezhooktool.xposed.common.BeforeChainStage
import io.github.lingqiqi5211.ezhooktool.xposed.common.ChainStage
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookChain
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookStageException
import io.github.lingqiqi5211.ezhooktool.xposed.common.InterceptChainStage
import io.github.lingqiqi5211.ezhooktool.xposed.common.ReplaceChainStage
import java.lang.reflect.Executable
import java.util.function.Consumer
import java.util.function.Function

/** Hook 回调签名。 */
typealias HookCallback = (HookParam) -> Unit

/** libxposed API 101 hook DSL 构造器。 */
class HookFactory internal constructor(
    private val target: Executable,
) {
    private val stages = mutableListOf<ChainStage>()
    private var priority: Int = XposedInterface.PRIORITY_DEFAULT
    private var exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT

    /**
     * 注册 before 回调。
     *
     * @param callback 原始调用前执行的回调
     */
    fun before(callback: HookCallback) {
        stages += BeforeChainStage(callback)
    }

    /**
     * 注册 Java `Consumer` 形式的 before 回调。
     *
     * @param callback 原始调用前执行的 Java 回调
     */
    fun before(callback: Consumer<HookParam>) {
        stages += BeforeChainStage { callback.accept(it) }
    }

    /**
     * 注册 after 回调。
     *
     * @param callback 原始调用后执行的回调
     */
    fun after(callback: HookCallback) {
        stages += AfterChainStage(callback)
    }

    /**
     * 注册 Java `Consumer` 形式的 after 回调。
     *
     * @param callback 原始调用后执行的 Java 回调
     */
    fun after(callback: Consumer<HookParam>) {
        stages += AfterChainStage { callback.accept(it) }
    }

    /**
     * 用自定义返回值替换原始实现。
     *
     * @param callback 生成替代返回值的回调
     */
    fun replace(callback: (HookParam) -> Any?) {
        stages += ReplaceChainStage(callback)
    }

    /**
     * 用 Java `Function` 形式替换原始实现。
     *
     * @param callback 生成替代返回值的 Java 回调
     */
    fun replace(callback: Function<HookParam, Any?>) {
        stages += ReplaceChainStage { callback.apply(it) }
    }

    /**
     * 注册 around 回调，可自行决定是否继续原始调用。
     *
     * @param callback 接收 [XposedInterface.Chain] 的 around 回调
     */
    fun intercept(callback: (XposedInterface.Chain) -> Any?) {
        stages += InterceptChainStage(callback)
    }

    /**
     * 注册 libxposed 原生 `Hooker` 形式的 around 回调。
     *
     * @param callback libxposed 原生 hooker
     */
    fun intercept(callback: XposedInterface.Hooker) {
        stages += InterceptChainStage { callback.intercept(it) }
    }

    /** 中断原始调用并返回 `null`。 */
    fun interrupt() {
        returnConstant(null)
    }

    /**
     * 中断原始调用并返回固定值。
     *
     * @param value 要返回给调用方的固定值
     */
    fun returnConstant(value: Any?) {
        stages += ReplaceChainStage { value }
    }

    /**
     * 设置 hook 优先级。
     *
     * @param priority 数值越大越先执行
     */
    fun priority(priority: Int) {
        this.priority = priority
    }

    /**
     * 设置 libxposed 异常处理模式。
     *
     * @param mode hook 过程中异常的处理策略
     */
    fun exceptionMode(mode: XposedInterface.ExceptionMode) {
        exceptionMode = mode
    }

    internal fun create(): XposedInterface.HookHandle {
        require(stages.isNotEmpty()) { "No hook callback specified" }
        val hooker = buildHooker(target, stages.toList())
        return EzXposed.base.hook(target)
            .setPriority(priority)
            .setExceptionMode(exceptionMode)
            .intercept(hooker)
    }
}

/**
 * 把若干 [ChainStage] 包成单个 [XposedInterface.Hooker]，并在 [EzXposed.safeMode] 打开时保护原始调用。
 *
 * 对外不暴露，供 [HookFactory.create] 和 [replaceHook] 共用，确保替换后的 hook 沿用同样的安全语义。
 */
internal fun buildHooker(
    target: Executable,
    stages: List<ChainStage>,
): XposedInterface.Hooker {
    val hookChain = HookChain(stages)
    return XposedInterface.Hooker { chain ->
        if (!EzXposed.safeMode) {
            hookChain.invoke(chain)
        } else {
            try {
                hookChain.invoke(chain)
            } catch (t: HookStageException) {
                EzReflect.logger.error("Hook", "${t.phase} hook failed for $target", t.cause ?: t)
                t.fallback()
            }
        }
    }
}
