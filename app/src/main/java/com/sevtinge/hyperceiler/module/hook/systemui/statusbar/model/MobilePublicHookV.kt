package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.telephony.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.miuiCellularIconVM
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.card1
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.card2
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.hideRoaming
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.isEnableDouble
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.StateFlowHelper.setStateFlowValue
import java.util.function.*

class MobilePublicHookV : BaseHook() {
    override fun init() {
        // findClass(
        //     "com.android.systemui.statusbar.connectivity.NetworkControllerImpl"
        // ).setStaticObjectField("DEBUG", true)

        hookAllConstructors(miuiCellularIconVM, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                val cellularIcon = param.thisObject
                val mobileIconInteractor = param.args[2]
                val subId = mobileIconInteractor.getObjectFieldAs<Int>("subId")

                val javaAdapter = Dependency.mMiuiLegacyDependency
                    ?.getObjectField("mCentralSurfaces")
                    ?.callMethod("get")
                    ?.getObjectField("mJavaAdapter")

                // 双排信号
                if (isEnableDouble) {
                    val isVisible = newReadonlyStateFlow(false)
                    cellularIcon.setObjectField("isVisible", isVisible)

                    val activeSubId = Dependency.mMiuiLegacyDependency
                        ?.getObjectField("mOperatorCustomizedPolicy")
                        ?.callMethod("get")
                        ?.getObjectField("mobileIcons")
                        ?.getObjectField("activeMobileDataSubscriptionId")

                    javaAdapter?.callMethod(
                        "alwaysCollectFlow",
                        activeSubId,
                        Consumer<Int> {
                            setStateFlowValue(isVisible, subId == it)
                        }
                    )
                } else {
                    val getSlotIndex = SubscriptionManager.getSlotIndex(subId)
                    if ((card1 && getSlotIndex == 0) || (card2 && getSlotIndex == 1)) {
                        cellularIcon.setObjectField("isVisible", newReadonlyStateFlow(false))
                    }
                }

                if (hideIndicator) {
                    cellularIcon.setObjectField("inOutVisible", newReadonlyStateFlow(false))
                }
                if (hideRoaming) {
                    cellularIcon.setObjectField("smallRoamVisible", newReadonlyStateFlow(false))
                    cellularIcon.setObjectField("mobileRoamVisible", newReadonlyStateFlow(false))
                }
                // 隐藏 hd
                updateIconState(param, "smallHdVisible", "system_ui_status_bar_icon_small_hd")
                updateIconState(param, "volteVisibleCn", "system_ui_status_bar_icon_big_hd")
                updateIconState(param, "volteVisibleGlobal", "system_ui_status_bar_icon_big_hd")
            }
        })
    }

    private fun updateIconState(param: MethodHookParam, fieldName: String, key: String) {
        val opt = mPrefsMap.getStringAsInt(key, 0)
        if (opt != 0) {
            val value = when (opt) {
                1 -> true
                else -> false
            }
            param.thisObject.setObjectField(fieldName, newReadonlyStateFlow(value))
        }
    }
}