package com.sevtinge.cemiuiler.module.home.recent

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.callStaticMethod
import com.sevtinge.cemiuiler.utils.findClass
import com.sevtinge.cemiuiler.utils.hookAfterAllMethods
import com.sevtinge.cemiuiler.utils.hookBeforeMethod

object BlurLevel : BaseHook() {
    override fun init() {

        val blurLevel = mPrefsMap.getStringAsInt("home_recent_blur_level", 6)
        if (blurLevel == 4) {
            findMethod("com.miui.home.launcher.common.BlurUtils") {
                name == "getBlurType"
            }.hookReturnConstant(0)
            findMethod("com.miui.home.launcher.common.BlurUtils") {
                name == "isUseCompleteBlurOnDev"
            }.hookReturnConstant(false)
            "com.miui.home.launcher.common.DeviceLevelUtils".hookBeforeMethod("isUseSimpleAnim") {
                it.result = true
            }
        } else if (blurLevel == 5) {
            val blurClass = "com.miui.home.launcher.common.BlurUtils".findClass()
            val navStubViewClass = "com.miui.home.recents.NavStubView".findClass()
            val applicationClass = "com.miui.home.launcher.Application".findClass()
            navStubViewClass.hookAfterAllMethods("onTouchEvent") {
                val mLauncher = applicationClass.callStaticMethod("getLauncher") as Activity
                blurClass.callStaticMethod("fastBlur", 1.0f, mLauncher.window, true, 500L)
            }
            "com.miui.home.launcher.common.DeviceLevelUtils".hookBeforeMethod("isUseSimpleAnim") {
                it.result = false
            }
            findMethod("com.miui.home.launcher.common.BlurUtils") {
                name == "getBlurType"
            }.hookBefore {
                when (blurLevel) {
                    5 -> it.result = 2
                }
            }
        } else {
            "com.miui.home.launcher.common.DeviceLevelUtils".hookBeforeMethod("isUseSimpleAnim") {
                it.result = false
            }
            findMethod("com.miui.home.launcher.common.BlurUtils") {
                name == "getBlurType"
            }.hookBefore {
                when (blurLevel) {
                    0 -> it.result = 2
                    2 -> it.result = 1
                    3 -> it.result = 0
                }
            }
            findMethod("com.miui.home.launcher.common.BlurUtils") {
                name == "isUseCompleteBlurOnDev"
            }.hookBefore {
                when (blurLevel) {
                    1 -> it.result = true
                }
            }
        }

    }
}