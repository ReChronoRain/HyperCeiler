package com.sevtinge.cemiuiler.module.systemui.statusbar.icon.all;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class StatusBarIconAtRight extends BaseHook {

    Class<?> mMiuiEndIconManager;
    Class<?> mMiuiPhoneStatusBarView;

    boolean isNetworkSpeedAtRightEnable;
    boolean isAlarmClockAtRightEnable;
    boolean isNFCAtRightEnable;
    boolean isVolmeAtRightEnable;
    boolean isZenAtRightEnable;

    @Override
    public void init() {

        mMiuiEndIconManager = findClass("com.android.systemui.statusbar.phone.MiuiEndIconManager");
        mMiuiPhoneStatusBarView = findClassIfExists("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView");

        isNetworkSpeedAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_network_speed_at_right");
        isAlarmClockAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_alarm_clock_at_right");
        isNFCAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_nfc_at_right");
        isVolmeAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_volume_at_right");
        isZenAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_zen_at_right");

        findAndHookMethod("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", "setIconVisibility", String.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String slot = (String) param.args[0];
                boolean isAlarmClockIcon = "alarm_clock".equals(slot) && isAlarmClockAtRightEnable;
                boolean isNFCIcon = "nfc".equals(slot) && isNFCAtRightEnable;
                boolean isVolumeIcon = "volume".equals(slot) && isVolmeAtRightEnable;
                boolean isZenIcon = "zen".equals(slot) && isZenAtRightEnable;

                if (isAlarmClockIcon || isNFCIcon || isVolumeIcon || isZenIcon) {
                    param.args[1] = false;
                }
            }
        });


        ArrayList<String> rightBlockList = (ArrayList<String>) XposedHelpers.getStaticObjectField(mMiuiEndIconManager, "RIGHT_BLOCK_LIST");

        if (isNetworkSpeedAtRightEnable) {
            rightBlockList.remove("network_speed");
        }
        if (isAlarmClockAtRightEnable) {
            rightBlockList.remove("alarm_clock");
        }
        if (isNFCAtRightEnable) {
            rightBlockList.remove("nfc");
        }
        if (isVolmeAtRightEnable) {
            rightBlockList.remove("volume");
        }
        if (isZenAtRightEnable) {
            rightBlockList.remove("zen");
        }

        XposedHelpers.setStaticObjectField(mMiuiEndIconManager, "RIGHT_BLOCK_LIST", rightBlockList);

        if (isNetworkSpeedAtRightEnable) {
            hookAllMethods(mMiuiPhoneStatusBarView, "updateCutoutLocation", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    int mCurrentStatusBarType = (int) XposedHelpers.getObjectField(param.thisObject, "mCurrentStatusBarType");
                    if (mCurrentStatusBarType == 1) {
                        Object mDripNetworkSpeedView = XposedHelpers.getObjectField(param.thisObject, "mDripNetworkSpeedView");
                        XposedHelpers.callMethod(mDripNetworkSpeedView, "setBlocked", true);
                    }
                }
            });

            hookAllMethods("com.android.systemui.statusbar.policy.NetworkSpeedController", "setDripNetworkSpeedView", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.args[0] = null;
                }
            });
        }
    }
}
