package com.sevtinge.hyperceiler.module.hook.home.other

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.findClass
import com.sevtinge.hyperceiler.utils.hookBeforeAllMethods

object BlurRadius : BaseHook() {
    override fun init() {

        val value = mPrefsMap.getInt("home_other_blur_radius", 100).toFloat() / 100
        if (value == 1f) return
        val blurUtilsClass = "com.miui.home.launcher.common.BlurUtils".findClass()
        blurUtilsClass.hookBeforeAllMethods("fastBlur") {
            it.args[0] = it.args[0] as Float * value
        }

    }
}
