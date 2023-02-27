package com.sevtinge.cemiuiler.module.systemframework

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

class VolumeMediaSteps : BaseHook(){
    override fun init() {
        val mediaStepsSwitch = mPrefsMap.getInt("system_framework_volume_media_steps",15)>15
        val mediaSteps = mPrefsMap.getInt("system_framework_volume_media_steps", 15)

        findMethod("android.os.SystemProperties") {
            name == "getInt" && returnType == Int::class.java
        }.hookBefore {
            when (it.args[0] as String) {
                "ro.config.media_vol_steps" -> if (mediaStepsSwitch) it.result = mediaSteps
            }
        }

    }
}