package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.setObjectField
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object FixMediaControlPanel : BaseHook() {
    override fun init() {
        try {
            EzXHelper.initHandleLoadPackage(lpparam)
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel",
                lpparam.classLoader,
                "setArtwork",
                XposedHelpers.findClass("com.android.systemui.media.MediaData", lpparam.classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.thisObject.setObjectField("mCurrentKey", "")
                    }
                })
        } catch (t: Throwable) {
            logE(t)
        }
    }
}
