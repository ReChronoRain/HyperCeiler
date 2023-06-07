package com.sevtinge.cemiuiler.module.screenrecorder

import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class ForceSupportPlaybackCapture : BaseHook() {
    override fun init() {
        // if (!xPrefs.getBoolean("force_support_playbackcapture", true)) return

        XposedHelpers.findAndHookMethod("android.os.SystemProperties",
            lpparam.classLoader,
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == "ro.vendor.audio.playbackcapture.screen")
                        param.result = true
                }
            })
    }
}
