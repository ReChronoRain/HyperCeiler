package com.sevtinge.cemiuiler.module.systemui.statusbar;

import android.view.View;
import android.widget.TextView;

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

        findAndHookMethod(mStatusBarMobileView, "initViewState", mMobileIconState, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
            }
        });

        findAndHookMethod(mStatusBarMobileView, "updateState", mMobileIconState, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                updateIconState(param, "mSmallHd", "system_ui_status_bar_icon_small_hd");
                updateIconState(param, "mVolte", "system_ui_status_bar_icon_big_hd");
            }
        });

        hookAllMethods(mStatusBarMobileView, "applyMobileState", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                int qpt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_type", 0);
                View mMobileType = (View) XposedHelpers.getObjectField(param.thisObject, "mMobileType");
                if (qpt > 0) {
                    boolean isMobileConnected = false;
                    TextView mMobileTypeSingle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle");
                    if (qpt == 1) {
                        mMobileTypeSingle.setVisibility(View.VISIBLE);
                        mMobileType.setVisibility(View.VISIBLE);
                    }
                    if (qpt == 3) {
                        isMobileConnected = (boolean) XposedHelpers.getObjectField(param.args[0], "dataConnected");
                    }
                    if (qpt == 2 || (qpt == 3 && !isMobileConnected)) {
                        mMobileTypeSingle.setVisibility(View.GONE);
                        mMobileType.setVisibility(View.GONE);
                    }
                }

            }
        });

        findAndHookMethod(mHDController, "update", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                int opt = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_new_hd", 0);
                if (opt > 0) {
                    XposedHelpers.setBooleanField(param.thisObject, "mWifiAvailable", opt == 1 ? false : opt == 2);
                }
            }
        });


        // 信号
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
                case 1 -> view.setVisibility(View.VISIBLE);
                case 2 -> view.setVisibility(View.GONE);
                case 3 -> {
                    view.setVisibility(View.GONE);
                    isMobileConnected = (boolean) XposedHelpers.getObjectField(param.args[0], "dataConnected");
                }
            }

            if (isMobileConnected) {
                view.setVisibility(View.VISIBLE);
            }
        }

    }
}
