package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers

class IconsFromSystemManager : BaseHook() {
    override fun init() {
        Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
            lpparam.classLoader,
            "setIcon", String::class.java,
            "com.android.internal.statusbar.StatusBarIcon",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    val slotName = param.args[0] as String
                    val stealth = slotName == "stealth" && mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_stealth")
                    val mute = slotName == "mute" && mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_mute")
                    val speakerphone = slotName == "speakerphone" && mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_speakerphone")
                    val call_record = slotName == "call_record" && mPrefsMap.getBoolean("system_ui_status_bar_hide_icon_call_record")

                    if (stealth || mute || speakerphone || call_record) {
                        XposedHelpers.setObjectField(param.args[1], "visible", false)
                    }
                }
            }
        )
    }
}
