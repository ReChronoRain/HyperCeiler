package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidVersion
import com.sevtinge.hyperceiler.utils.setBooleanField

object HideVoWiFiIcon : BaseHook() {
    override fun init() {
        var hide_vowifi = mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi")
        var hide_volte = mPrefsMap.getBoolean("system_ui_status_bar_icon_volte")
        if (isAndroidVersion(34)) {
            loadClass("com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig").constructors[0].createHook {
                after {
                    it.thisObject.setBooleanField("hideVowifi", hide_vowifi)
                    it.thisObject.setBooleanField("hideVolte", hide_volte)
                }
            }
        } else if (hide_vowifi) {
            loadClass("com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig").methodFinder().first {
                name == "getHideVowifi"
            }.createHook { returnConstant(true) }
        }
    }
}
