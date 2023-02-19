package com.sevtinge.cemiuiler.module.systemui.statusbar;

import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class WifiNetworkIndicator extends BaseHook {

    int mVisibility;

    Class<?> mStatusBarWifiView;

    @Override
    public void init() {

        mStatusBarWifiView = findClassIfExists("com.android.systemui.statusbar.StatusBarWifiView");
        int mWifiNetworkIndicator = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_network_indicator", 0);

        if (mWifiNetworkIndicator == 1) {
            mVisibility = View.VISIBLE;
        } else if (mWifiNetworkIndicator == 2) {
            mVisibility = View.INVISIBLE;
        }

        MethodHook hideWifiActivity = new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                Object mWifiActivityView = XposedHelpers.getObjectField(param.thisObject, "mWifiActivityView");
                XposedHelpers.callMethod(mWifiActivityView, "setVisibility", mVisibility);
            }
        };

        hookAllMethods(mStatusBarWifiView, "applyWifiState", hideWifiActivity);
    }
}
