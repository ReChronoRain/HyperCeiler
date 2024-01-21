package com.sevtinge.hyperceiler.module.hook.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object AnimDurationRatio : BaseHook() {
    override fun init() {
        var value1 = mPrefsMap.getInt("home_title_animation_speed", 100).toFloat()
        var value2 = mPrefsMap.getInt("home_recent_animation_speed", 100).toFloat()
        if (value1 != 100f) {
            value1 = value1 / 100f
            loadClass("com.miui.home.recents.util.RectFSpringAnim").methodFinder().first {
                name == "getModifyResponse"
            }.createHook {
                before {
                    it.result = it.args[0] as Float * value1
                }
            }
        }
        if (value2 != 100f) {
            value2 = value2 / 100f
            loadClass("com.miui.home.launcher.common.DeviceLevelUtils").methodFinder().first {
                name == "getDeviceLevelTransitionAnimRatio"
            }.createHook {
                before {
                    it.result = value2
                }
            }
        }
    }
}
