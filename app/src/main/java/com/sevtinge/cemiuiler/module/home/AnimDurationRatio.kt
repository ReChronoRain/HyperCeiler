package com.sevtinge.cemiuiler.module.home

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

object AnimDurationRatio : BaseHook() {
    override fun init() {

        val value1 = mPrefsMap.getInt("home_title_animation_speed", 150).toFloat() / 100f
        val value2 = mPrefsMap.getInt("home_recent_animation_speed", 130).toFloat() / 100f
        findMethod("com.miui.home.recents.util.RectFSpringAnim") {
            name == "getModifyResponse"
        }.hookBefore {
            it.result = it.args[0] as Float * value1
        }

        findMethod("com.miui.home.launcher.common.DeviceLevelUtils") {
            name == "getDeviceLevelTransitionAnimRatio"
        }.hookBefore {
            it.result = value2
        }

    }
}