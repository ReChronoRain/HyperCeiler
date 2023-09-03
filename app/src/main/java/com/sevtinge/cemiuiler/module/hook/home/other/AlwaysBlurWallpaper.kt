package com.sevtinge.cemiuiler.module.hook.home.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object AlwaysBlurWallpaper : BaseHook() {
    override fun init() {

        // if (!mPrefsMap.getBoolean("home_blur_wallpaper")) return
        val value = mPrefsMap.getInt("home_blur_radius", 100)

        loadClass("com.miui.home.launcher.common.BlurUtils").methodFinder().first {
            name == "fastBlur" && parameterCount == 4
        }.createHook {
            before {
                it.args[0] = value.toFloat() / 100
                it.args[2] = true
            }
        }
    }
}
