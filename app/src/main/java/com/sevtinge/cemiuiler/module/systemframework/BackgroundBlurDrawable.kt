package com.sevtinge.cemiuiler.module.systemframework

import android.graphics.Canvas
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

class BackgroundBlurDrawable :IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        val classLoader = startupParam.javaClass.classLoader
        val BackgroundBlurDrawableClass = classLoader?.let {
            HookUtils.getClass(
                "com.android.internal.graphics.drawable.BackgroundBlurDrawable",
                it
            )
        } ?: return
        // 为 BackgroundBlurDrawable 应当增加一个判断
        // 此处应该可以为AOSP提交修复补丁
        XposedBridge.hookAllMethods(
            BackgroundBlurDrawableClass,
            "draw",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val canvas = param.args[0] as Canvas
                    if(!canvas.isHardwareAccelerated){
                        HookUtils.log("BackgroundBlurDrawable canvas is not HardwareAccelerated.")
                        param.result = null
                    }
                }
            })
    }
}