package com.voyager.star.hooks.apps

import android.os.Build
import com.voyager.star.hooks.rules.mediaeditor.*
import com.voyager.star.utils.AppRegister
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

object MediaEditor : AppRegister() {
    override val packageName: String = "com.miui.mediaeditor"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("Voyager-Test: MediaEditor Hook success!")
        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.TIRAMISU -> {
                autoInitHooks(
                    lpparam,
                    FilterManagerAll, // 解锁大师滤镜
                )
            }

            Build.VERSION_CODES.S -> {
                autoInitHooks(
                    lpparam,
                    FilterManagerAll, // 解锁大师滤镜
                )
            }
        }
    }
}