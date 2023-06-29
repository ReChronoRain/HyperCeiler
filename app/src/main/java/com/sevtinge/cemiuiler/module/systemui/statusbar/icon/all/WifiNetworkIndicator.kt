package com.sevtinge.cemiuiler.module.systemui.statusbar.icon.all

import android.view.View
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object WifiNetworkIndicator : BaseHook() {
    var mVisibility = 0
    private var mStatusBarWifiView: Class<*>? = null
    override fun init() {
        mStatusBarWifiView = findClassIfExists("com.android.systemui.statusbar.StatusBarWifiView")
        val mWifiNetworkIndicator =
            mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_network_indicator", 0)

        when (mWifiNetworkIndicator) {
            1 -> mVisibility = View.VISIBLE
            2 -> mVisibility = View.INVISIBLE
        }

        val hideWifiActivity: MethodHook = object : MethodHook() {
            override fun after(param: MethodHookParam) {
                val mWifiActivityView =
                    XposedHelpers.getObjectField(param.thisObject, "mWifiActivityView")
                XposedHelpers.callMethod(mWifiActivityView, "setVisibility", mVisibility)
            }
        }
        hookAllMethods(mStatusBarWifiView, "applyWifiState", hideWifiActivity)
    }
}
