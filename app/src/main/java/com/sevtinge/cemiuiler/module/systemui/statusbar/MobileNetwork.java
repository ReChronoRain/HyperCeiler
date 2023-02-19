package com.sevtinge.cemiuiler.module.systemui.statusbar;

import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class MobileNetwork extends BaseHook {

    Class<?> mStatusBarMobileView;
    Class<?> mMobileIconState;
    Class<?> mHDController;

    @Override
    public void init() {

        mStatusBarMobileView = findClassIfExists("com.android.systemui.statusbar.StatusBarMobileView");
        mMobileIconState = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarSignalPolicy$MobileIconState");

        mHDController = findClassIfExists("com.android.systemui.statusbar.policy.HDController");

        findAndHookMethod(mStatusBarMobileView,"initViewState", mMobileIconState, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                updateIconState(param, "mVolte","system_ui_status_bar_icon_big_hd");
                updateIconState(param, "mMobileType", "system_ui_status_bar_icon_mobile_network_type");
            }
        });

        findAndHookMethod(mStatusBarMobileView,"updateState", mMobileIconState, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                updateIconState(param, "mVolte","system_ui_status_bar_icon_big_hd");
                updateIconState(param, "mMobileType", "system_ui_status_bar_icon_mobile_network_type");

            }
        });

        findAndHookMethod(mHDController, "update", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_new_hd", 0);
                if (opt > 0) {
                    XposedHelpers.setBooleanField(param.thisObject, "mWifiAvailable", opt == 1 ? false : opt == 2);
                }
            }
        });


        //信号
        /*hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", "applyMobileState", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(param.thisObject, "setVisibility", View.GONE);
            }
        });*/
    }

    private void updateIconState(MethodHookParam param, String fieldName, String key) {
        boolean isMobileConnected = false;
        int opt = mPrefsMap.getStringAsInt(key, 0);
        if (opt != 0) {
            View view = (View) XposedHelpers.getObjectField(param.thisObject, fieldName);
            switch (opt) {
                case 1 :
                    view.setVisibility(View.VISIBLE);
                    break;
                case 2 :
                    view.setVisibility(View.GONE);
                    break;
                case 3 :
                    view.setVisibility(View.GONE);
                    isMobileConnected = (boolean) XposedHelpers.getObjectField(param.args[0], "dataConnected");
                    break;
            }

            if (isMobileConnected) {
                view.setVisibility(View.VISIBLE);
            }
        }

    }
}
