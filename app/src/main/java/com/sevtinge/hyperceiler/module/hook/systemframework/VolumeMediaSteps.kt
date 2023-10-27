package com.sevtinge.hyperceiler.module.hook.systemframework

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

class VolumeMediaSteps : BaseHook() {
    override fun init() {
        val mediaStepsSwitch = mPrefsMap.getInt("system_framework_volume_media_steps", 15) > 15
        val mediaSteps = mPrefsMap.getInt("system_framework_volume_media_steps", 15)

        loadClass("android.os.SystemProperties").methodFinder().first {
            name == "getInt" && returnType == Int::class.java
        }.createHook {
            before {
                when (it.args[0] as String) {
                    "ro.config.media_vol_steps" -> if (mediaStepsSwitch) it.result = mediaSteps
                }
            }
        }

    }
}
