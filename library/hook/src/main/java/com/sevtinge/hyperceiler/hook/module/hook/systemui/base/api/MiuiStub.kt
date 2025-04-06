package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

import android.content.Context
import android.os.Handler
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.sevtinge.hyperceiler.hook.utils.callMethodAs
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.getStaticObjectFieldAs
import java.util.concurrent.Executor

/**
 * only for HyperOS2
 */
@Suppress("unused")
object MiuiStub {
    private val INSTANCE by lazy {
        loadClass("miui.stub.MiuiStub").getStaticObjectFieldAs<Any>("INSTANCE")
    }

    lateinit var javaAdapter: JavaAdapter

    val baseProvider by lazy {
        BaseProvider(INSTANCE.getObjectFieldAs("mBaseProvider"))
    }

    val miuiModuleProvider by lazy {
        MiuiModuleProvider(INSTANCE.getObjectFieldAs("mMiuiModuleProvider"))
    }

    val sysUIProvider by lazy {
        SysUIProvider(INSTANCE.getObjectFieldAs("mSysUIProvider"))
    }

    @JvmStatic
    fun createHook() {
        loadClass("com.android.systemui.util.kotlin.JavaAdapter").constructorFinder()
            .first()
            .createAfterHook {
                javaAdapter = JavaAdapter(it.thisObject)
            }
    }

    override fun toString(): String = INSTANCE.toString()

    class BaseProvider(instance: Any) : BaseReflectObject(instance) {
        val mainHandler by lazy {
            instance.getObjectFieldAs<Handler>("mMainHandler")
        }

        val bgHandler by lazy {
            instance.getObjectFieldAs<Handler>("mBgHandler")
        }

        val context by lazy {
            instance.getObjectFieldAs<Context>("mContext")
        }

        val uiBackgroundExecutor by lazy {
            instance.getObjectFieldAs<Executor>("mUiBackgroundExecutor")
        }
    }

    class MiuiModuleProvider(instance: Any) : BaseReflectObject(instance)

    class SysUIProvider(instance: Any) : BaseReflectObject(instance) {
        val flashlightController by lazy {
            FlashlightController(
                instance.getObjectFieldAs<Any>("mFlashlightController").callMethodAs("get")
            )
        }

        val activityStarter by lazy {
            ActivityStarter(
                instance.getObjectFieldAs<Any>("mActivityStarter").callMethodAs("get")
            )
        }
    }
}
