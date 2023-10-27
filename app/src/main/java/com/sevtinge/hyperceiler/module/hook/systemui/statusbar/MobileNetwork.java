package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

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
                boolean singleMobileType = mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable");
                boolean hideIndicator = mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator");
                View mMobileType = getMobileType(param, qpt, singleMobileType);
                // 隐藏移动网络活动指示器
                View mLeftInOut = (View) XposedHelpers.getObjectField(param.thisObject, "mLeftInOut");
                if (hideIndicator) {
                    View mRightInOut = (View) XposedHelpers.getObjectField(param.thisObject, "mRightInOut");
                    mLeftInOut.setVisibility(View.GONE);
                    mRightInOut.setVisibility(View.GONE);
                }
                if (mMobileType.getVisibility() == View.GONE && mLeftInOut.getVisibility() == View.GONE) {
                    View mMobileLeftContainer = (View) XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer");
                    mMobileLeftContainer.setVisibility(View.GONE);
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

    private static View getMobileType(MethodHookParam param, int qpt, boolean singleMobileType) {
        View mMobileType = (View) XposedHelpers.getObjectField(param.thisObject, "mMobileType");
        boolean dataConnected = (boolean) XposedHelpers.getObjectField(param.args[0], "dataConnected");
        if (qpt > 0) {
            if (qpt == 1) {
                if (singleMobileType) {
                    TextView mMobileTypeSingle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle");
                    mMobileTypeSingle.setVisibility(View.VISIBLE);
                } else {
                    mMobileType.setVisibility(View.VISIBLE);
                }
            }
            if (qpt == 3 && !dataConnected) {
                if (singleMobileType) {
                    TextView mMobileTypeSingle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle");
                    mMobileTypeSingle.setVisibility(View.GONE);
                } else {
                    mMobileType.setVisibility(View.GONE);
                }
            }
            if (qpt == 2) {
                if (singleMobileType) {
                    TextView mMobileTypeSingle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle");
                    mMobileTypeSingle.setVisibility(View.GONE);
                } else {
                    mMobileType.setVisibility(View.GONE);
                }
            }
        }
        return mMobileType;
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
