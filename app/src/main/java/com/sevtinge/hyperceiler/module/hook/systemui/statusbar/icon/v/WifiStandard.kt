package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.v

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.getObjectField
import com.sevtinge.hyperceiler.utils.setObjectField

object WifiStandard : BaseHook() {
    val showWifi by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0)
    }

    override fun init() {
        loadClass("com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModel")
            .constructorFinder().first()
            .createAfterHook {
                if (showWifi == 1) {
                    val getValue = it.thisObject.getObjectField("wifiStandard")!!.callMethod("getValue", newReadonlyStateFlow(0))
                    it.thisObject.setObjectField("wifiStandard", newReadonlyStateFlow(getValue))
                } else if (showWifi == 2) {
                    it.thisObject.setObjectField("wifiStandard", newReadonlyStateFlow(0))
                }
            }

        loadClass("com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModel\$wifiStandard\$1")
            .methodFinder()
            .filterByName("invokeSuspend")
            .first()
            .createAfterHook { 
                if (showWifi == 1) {
                   it.thisObject.getObjectField("this$0")!!
                       .getObjectField("wifiStandard")!!
                       .getObjectField("$\$delegate_0")!!
                       .callMethod("setValue", newReadonlyStateFlow(it.result))
                }
            }
    }
}