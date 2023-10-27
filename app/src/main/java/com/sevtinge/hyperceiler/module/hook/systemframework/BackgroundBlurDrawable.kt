package com.sevtinge.hyperceiler.module.hook.systemframework

import android.graphics.Canvas
import com.sevtinge.hyperceiler.utils.HookUtils
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

class BackgroundBlurDrawable : IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        val classLoader = startupParam.javaClass.classLoader
        val mBackgroundBlurDrawableClass = classLoader?.let {
            HookUtils.getClass("com.android.internal.graphics.drawable.BackgroundBlurDrawable", it)
        } ?: return
        // 为 BackgroundBlurDrawable 应当增加一个判断
        // 此处应该可以为AOSP提交修复补丁
        XposedBridge.hookAllMethods(
            mBackgroundBlurDrawableClass,
            "draw",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val canvas = param.args[0] as Canvas
                    if (!canvas.isHardwareAccelerated) {
                        XposedLogUtils.logI("BackgroundBlurDrawable canvas is not HardwareAccelerated.")
                        param.result = null
                    }
                }
            })
    }
}
