@file:Suppress("unused")

package io.github.kyuubiran.ezxhelper.xposed.dsl

import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.kyuubiran.ezxhelper.xposed.common.AfterChainStage
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeChainStage
import io.github.kyuubiran.ezxhelper.xposed.common.ChainStage
import io.github.kyuubiran.ezxhelper.xposed.common.HookChain
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam
import io.github.kyuubiran.ezxhelper.xposed.common.InterceptChainStage
import io.github.kyuubiran.ezxhelper.xposed.common.ReplaceChainStage
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.util.function.Consumer

class HookFactory private constructor(private val target: Executable) {

    private val stages = mutableListOf<ChainStage>()
    private var exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT

    /**
     * Hook method before invoke.
     */
    fun before(callback: ((HookParam) -> Unit)?) {
        callback ?: return
        stages += BeforeChainStage(callback)
    }

    fun before(callback: Consumer<HookParam>?) {
        callback ?: return
        before { param -> callback.accept(param) }
    }

    fun exceptionMode(mode: XposedInterface.ExceptionMode) {
        exceptionMode = mode
    }

    /**
     * Hook method after invoked.
     */
    fun after(callback: ((HookParam) -> Unit)?) {
        callback ?: return
        stages += AfterChainStage(callback)
    }

    fun after(callback: Consumer<HookParam>?) {
        callback ?: return
        after { param -> callback.accept(param) }
    }

    /**
     * Replace the method result.
     */
    fun replace(callback: (param: HookParam) -> Any?) {
        stages += ReplaceChainStage(callback)
    }

    /**
     * Interrupt the method and return null.
     */
    fun interrupt() {
        returnConstant(null)
    }

    /**
     * Replace the result of the method with a constant.
     */
    fun returnConstant(constant: Any?) {
        stages += ReplaceChainStage { constant }
    }

    private fun create(priority: Int = XposedInterface.PRIORITY_DEFAULT): HookHandle {
        require(stages.isNotEmpty()) { "No hook callback specified" }
        val hookChain = HookChain(stages.toList())
        stages.clear()
        return EzXposed.base
            .hook(target)
            .setPriority(priority)
            .setExceptionMode(exceptionMode)
            .intercept(Hooker { chain -> hookChain.invoke(chain) })
    }

    @Suppress("ClassName")
    companion object `-Static` {
        @JvmSynthetic
        private fun <T : Executable> T.internalCreateHook(
            priority: Int,
            exceptionMode: XposedInterface.ExceptionMode,
            block: HookFactory.() -> Unit,
        ): HookHandle = HookFactory(this).apply { this.exceptionMode = exceptionMode }.also(block).create(priority)

        @JvmSynthetic
        private fun <T : Executable> T.internalCreateHook(
            priority: Int,
            exceptionMode: XposedInterface.ExceptionMode,
            block: Consumer<HookFactory>,
        ): HookHandle = HookFactory(this).apply { this.exceptionMode = exceptionMode }.also { block.accept(it) }.create(priority)

        @JvmSynthetic
        private fun <T : Executable> T.internalCreateBeforeHook(
            priority: Int,
            exceptionMode: XposedInterface.ExceptionMode,
            block: (HookParam) -> Unit,
        ): HookHandle = HookFactory(this).apply { this.exceptionMode = exceptionMode; before(block) }.create(priority)

        @JvmSynthetic
        private fun <T : Executable> T.internalCreateAfterHook(
            priority: Int,
            exceptionMode: XposedInterface.ExceptionMode,
            block: (HookParam) -> Unit,
        ): HookHandle = HookFactory(this).apply { this.exceptionMode = exceptionMode; after(block) }.create(priority)

        @JvmSynthetic
        private fun <T : Executable> T.internalCreateIntercept(
            priority: Int,
            exceptionMode: XposedInterface.ExceptionMode,
            block: (XposedInterface.Chain) -> Any?,
        ): HookHandle = HookFactory(this).apply { this.exceptionMode = exceptionMode; stages += InterceptChainStage(block) }.create(priority)

        @JvmName("-createMethodHook")
        @JvmSynthetic
        fun Method.createHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: HookFactory.() -> Unit,
        ): HookHandle = internalCreateHook(priority, exceptionMode, block)

        @JvmName("-createMethodBeforeHook")
        @JvmSynthetic
        fun Method.createBeforeHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = internalCreateBeforeHook(priority, exceptionMode, block)

        @JvmName("-createMethodAfterHook")
        @JvmSynthetic
        fun Method.createAfterHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = internalCreateAfterHook(priority, exceptionMode, block)

        @JvmName("-hookMethod")
        @JvmSynthetic
        fun Method.hook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (XposedInterface.Chain) -> Any?,
        ): HookHandle = internalCreateIntercept(priority, exceptionMode, block)

        @JvmName("-createConstructorHook")
        @JvmSynthetic
        fun Constructor<*>.createHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: HookFactory.() -> Unit,
        ): HookHandle = internalCreateHook(priority, exceptionMode, block)

        @JvmName("-createConstructorBeforeHook")
        @JvmSynthetic
        fun Constructor<*>.createBeforeHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = internalCreateBeforeHook(priority, exceptionMode, block)

        @JvmName("-createConstructorAfterHook")
        @JvmSynthetic
        fun Constructor<*>.createAfterHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = internalCreateAfterHook(priority, exceptionMode, block)

        @JvmName("-hookConstructor")
        @JvmSynthetic
        fun Constructor<*>.hook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (XposedInterface.Chain) -> Any?,
        ): HookHandle = internalCreateIntercept(priority, exceptionMode, block)

        @JvmName("-createMethodHooks")
        @JvmSynthetic
        fun Iterable<Method>.createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: HookFactory.() -> Unit,
        ): List<HookHandle> = map { it.createHook(priority, exceptionMode, block) }

        @JvmName("-createMethodBeforeHooks")
        @JvmSynthetic
        fun Iterable<Method>.createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createBeforeHook(priority, exceptionMode, block) }

        @JvmName("-createMethodAfterHooks")
        @JvmSynthetic
        fun Iterable<Method>.createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createAfterHook(priority, exceptionMode, block) }

        @JvmName("-createMethodHooks")
        @JvmSynthetic
        fun Array<Method>.createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: HookFactory.() -> Unit,
        ): List<HookHandle> = map { it.createHook(priority, exceptionMode, block) }

        @JvmName("-createMethodBeforeHooks")
        @JvmSynthetic
        fun Array<Method>.createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createBeforeHook(priority, exceptionMode, block) }

        @JvmName("-createMethodAfterHooks")
        @JvmSynthetic
        fun Array<Method>.createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createAfterHook(priority, exceptionMode, block) }

        @JvmName("-createConstructorHooks")
        @JvmSynthetic
        fun Iterable<Constructor<*>>.createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: HookFactory.() -> Unit,
        ): List<HookHandle> = map { it.createHook(priority, exceptionMode, block) }

        @JvmName("-createConstructorBeforeHooks")
        @JvmSynthetic
        fun Iterable<Constructor<*>>.createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createBeforeHook(priority, exceptionMode, block) }

        @JvmName("-createConstructorAfterHooks")
        @JvmSynthetic
        fun Iterable<Constructor<*>>.createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createAfterHook(priority, exceptionMode, block) }

        @JvmName("-createConstructorHooks")
        @JvmSynthetic
        fun Array<Constructor<*>>.createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: HookFactory.() -> Unit,
        ): List<HookHandle> = map { it.createHook(priority, exceptionMode, block) }

        @JvmName("-createConstructorBeforeHooks")
        @JvmSynthetic
        fun Array<Constructor<*>>.createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createBeforeHook(priority, exceptionMode, block) }

        @JvmName("-createConstructorAfterHooks")
        @JvmSynthetic
        fun Array<Constructor<*>>.createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = map { it.createAfterHook(priority, exceptionMode, block) }

        @JvmName("createMethodHook")
        @JvmStatic
        @JvmOverloads
        fun createHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            method: Method,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: Consumer<HookFactory>,
        ): HookHandle = method.internalCreateHook(priority, exceptionMode, block)

        @JvmName("createMethodBeforeHook")
        @JvmStatic
        @JvmOverloads
        fun createBeforeHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            method: Method,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = method.internalCreateBeforeHook(priority, exceptionMode, block)

        @JvmName("createMethodAfterHook")
        @JvmStatic
        @JvmOverloads
        fun createAfterHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            method: Method,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = method.internalCreateAfterHook(priority, exceptionMode, block)

        @JvmName("hookMethod")
        @JvmStatic
        @JvmOverloads
        fun hook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            method: Method,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            hooker: XposedInterface.Hooker,
        ): HookHandle = EzXposed.base.hook(method).setPriority(priority).setExceptionMode(exceptionMode).intercept(hooker)

        @JvmName("createConstructorHook")
        @JvmStatic
        @JvmOverloads
        fun createHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctor: Constructor<*>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: Consumer<HookFactory>,
        ): HookHandle = ctor.internalCreateHook(priority, exceptionMode, block)

        @JvmName("createConstructorBeforeHook")
        @JvmStatic
        @JvmOverloads
        fun createBeforeHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctor: Constructor<*>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = ctor.internalCreateBeforeHook(priority, exceptionMode, block)

        @JvmName("createConstructorAfterHook")
        @JvmStatic
        @JvmOverloads
        fun createAfterHook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctor: Constructor<*>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): HookHandle = ctor.internalCreateAfterHook(priority, exceptionMode, block)

        @JvmName("hookConstructor")
        @JvmStatic
        @JvmOverloads
        fun hook(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctor: Constructor<*>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            hooker: XposedInterface.Hooker,
        ): HookHandle = EzXposed.base.hook(ctor).setPriority(priority).setExceptionMode(exceptionMode).intercept(hooker)

        @JvmName("createMethodHooks")
        @JvmStatic
        @JvmOverloads
        fun createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            methods: Iterable<Method>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: Consumer<HookFactory>,
        ): List<HookHandle> = methods.map { createHook(priority, it, exceptionMode, block) }

        @JvmName("createMethodBeforeHooks")
        @JvmStatic
        @JvmOverloads
        fun createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            methods: Iterable<Method>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = methods.map { createBeforeHook(priority, it, exceptionMode, block) }

        @JvmName("createMethodAfterHooks")
        @JvmStatic
        @JvmOverloads
        fun createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            methods: Iterable<Method>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = methods.map { createAfterHook(priority, it, exceptionMode, block) }

        @JvmName("createMethodHooks")
        @JvmStatic
        @JvmOverloads
        fun createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            methods: Array<Method>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: Consumer<HookFactory>,
        ): List<HookHandle> = methods.map { createHook(priority, it, exceptionMode, block) }

        @JvmName("createMethodBeforeHooks")
        @JvmStatic
        @JvmOverloads
        fun createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            methods: Array<Method>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = methods.map { createBeforeHook(priority, it, exceptionMode, block) }

        @JvmName("createMethodAfterHooks")
        @JvmStatic
        @JvmOverloads
        fun createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            methods: Array<Method>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = methods.map { createAfterHook(priority, it, exceptionMode, block) }

        @JvmName("createConstructorHooks")
        @JvmStatic
        @JvmOverloads
        fun createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctors: Iterable<Constructor<*>>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: Consumer<HookFactory>,
        ): List<HookHandle> = ctors.map { createHook(priority, it, exceptionMode, block) }

        @JvmName("createConstructorBeforeHooks")
        @JvmStatic
        @JvmOverloads
        fun createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctors: Iterable<Constructor<*>>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = ctors.map { createBeforeHook(priority, it, exceptionMode, block) }

        @JvmName("createConstructorAfterHooks")
        @JvmStatic
        @JvmOverloads
        fun createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctors: Iterable<Constructor<*>>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = ctors.map { createAfterHook(priority, it, exceptionMode, block) }

        @JvmName("createConstructorHooks")
        @JvmStatic
        @JvmOverloads
        fun createHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctors: Array<Constructor<*>>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: Consumer<HookFactory>,
        ): List<HookHandle> = ctors.map { createHook(priority, it, exceptionMode, block) }

        @JvmName("createConstructorBeforeHooks")
        @JvmStatic
        @JvmOverloads
        fun createBeforeHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctors: Array<Constructor<*>>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = ctors.map { createBeforeHook(priority, it, exceptionMode, block) }

        @JvmName("createConstructorAfterHooks")
        @JvmStatic
        @JvmOverloads
        fun createAfterHooks(
            priority: Int = XposedInterface.PRIORITY_DEFAULT,
            ctors: Array<Constructor<*>>,
            exceptionMode: XposedInterface.ExceptionMode = XposedInterface.ExceptionMode.DEFAULT,
            block: (HookParam) -> Unit,
        ): List<HookHandle> = ctors.map { createAfterHook(priority, it, exceptionMode, block) }
    }
}
