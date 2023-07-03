package com.sevtinge.cemiuiler.module.systemui.statusbar.icon.all

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object HideVoWiFiIcon : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.MiuiOperatorCustomizedPolicy\$MiuiOperatorConfig").methodFinder().first {
            name == "getHideVowifi"
        }.createHook { returnConstant(true) }
    }
}
