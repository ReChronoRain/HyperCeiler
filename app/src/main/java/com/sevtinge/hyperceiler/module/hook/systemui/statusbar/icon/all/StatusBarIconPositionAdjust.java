/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class StatusBarIconPositionAdjust extends BaseHook {

    boolean isMoveLeft;
    boolean isMoveCenter;
    boolean isMoveRight;

    String[] mSignalIcons;
    ArrayList<String> mSignalRelatedIcons;

    Class<?> mStatusBarIconList;
    Class<?> mSystemUIApplication;
    Class<?> mMiuiDripLeftStatusBarIconControllerImpl;

    boolean isWiFiAtLeftEnable;
    boolean isMobileNetworkAtLeftEnable;
    boolean isSwapWiFiAndMobileNetwork;

    boolean isNetworkSpeedAtRightEnable;
    boolean isAlarmClockAtRightEnable;
    boolean isNFCAtRightEnable;
    boolean isVolmeAtRightEnable;
    boolean isZenAtRightEnable;
    boolean isHeadsetAtRightEnable;

    @Override
    public void init() {

        mStatusBarIconList = findClassIfExists("com.android.systemui.statusbar.phone.StatusBarIconList");
        mSystemUIApplication = findClassIfExists("com.android.systemui.SystemUIApplication");
        mMiuiDripLeftStatusBarIconControllerImpl = findClassIfExists("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl");

        ArrayList<String> dripLeftIcons = new ArrayList<>();

        isWiFiAtLeftEnable = mPrefsMap.getBoolean("system_ui_status_bar_wifi_at_left");
        isMobileNetworkAtLeftEnable = mPrefsMap.getBoolean("system_ui_status_bar_mobile_network_at_left");

        isNetworkSpeedAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_network_speed_at_right");
        isAlarmClockAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_alarm_clock_at_right");
        isNFCAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_nfc_at_right");
        isVolmeAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_volume_at_right");
        isZenAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_zen_at_right");
        isHeadsetAtRightEnable = mPrefsMap.getBoolean("system_ui_status_bar_headset_at_right");

        isSwapWiFiAndMobileNetwork = mPrefsMap.getBoolean("system_ui_status_bar_swap_wifi_and_mobile_network");

        isMoveLeft = isWiFiAtLeftEnable || isMobileNetworkAtLeftEnable;
        isMoveRight = isNetworkSpeedAtRightEnable || isAlarmClockAtRightEnable || isNFCAtRightEnable || isVolmeAtRightEnable || isZenAtRightEnable || isHeadsetAtRightEnable;

        if (isWiFiAtLeftEnable && isMobileNetworkAtLeftEnable && !isSwapWiFiAndMobileNetwork) {
            mSignalIcons = new String[]{"no_sim", "mobile", "demo_mobile", "airplane", "hotspot", "slave_wifi", "wifi", "demo_wifi"};
        } else {
            mSignalIcons = new String[]{"hotspot", "slave_wifi", "wifi", "demo_wifi", "no_sim", "mobile", "demo_mobile", "airplane"};
            /*if (isWiFiAtLeftEnable) {
                mSignalIcons = new String[]{"hotspot", "slave_wifi", "wifi", "demo_wifi"};
            }

            if (isMobileNetworkAtLeftEnable) {
                mSignalIcons = new String[]{"no_sim", "mobile", "demo_mobile", "airplane"};
            }*/
        }

        mSignalRelatedIcons = new ArrayList<>(Arrays.asList(mSignalIcons));

        if (isMoveRight) {

            findAndHookMethod(mMiuiDripLeftStatusBarIconControllerImpl,
                "setIconVisibility", String.class, boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    String slot = (String) param.args[0];

                    boolean isAlarmClockIcon = "alarm_clock".equals(slot) && isAlarmClockAtRightEnable;
                    boolean isNFCIcon = "nfc".equals(slot) && isNFCAtRightEnable;
                    boolean isVolumeIcon = "volume".equals(slot) && isVolmeAtRightEnable;
                    boolean isZenIcon = "zen".equals(slot) && isZenAtRightEnable;
                    boolean isHeadsetIcon = "headset".equals(slot) && isHeadsetAtRightEnable;

                    if (isAlarmClockIcon || isNFCIcon || isVolumeIcon || isZenIcon || isHeadsetIcon) {
                        param.args[1] = false;
                    }
                }
            });

            findAndHookMethod(mSystemUIApplication, "onCreate", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    ArrayList<String> rightBlockList;
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    Class<?> MiuiEndIconManager = findClass("com.android.systemui.statusbar.phone.MiuiEndIconManager", lpparam.classLoader);
                    Object blockList = getStaticObjectFieldSilently(MiuiEndIconManager, "RIGHT_BLOCK_LIST");

                    Resources res = mContext.getResources();
                    if (blockList != null) {
                        rightBlockList = (ArrayList<String>) blockList;
                    } else {
                        @SuppressLint("DiscouragedApi") int blockResId = res.getIdentifier("config_drip_right_block_statusBarIcons", "array", lpparam.packageName);
                        rightBlockList = new ArrayList<>(Arrays.asList(res.getStringArray(blockResId)));
                    }
                    if (isNetworkSpeedAtRightEnable) {
                        rightBlockList.remove("network_speed");
                    }
                    if (isAlarmClockAtRightEnable) {
                        rightBlockList.remove("alarm_clock");
                    }
                    if (isVolmeAtRightEnable) {
                        rightBlockList.remove("volume");
                    }
                    if (isZenAtRightEnable) {
                        rightBlockList.remove("zen");
                    }
                    /*if (mPrefsMap.getBoolean("system_statusbar_btbattery_atright")) {
                        rightBlockList.remove("bluetooth_handsfree_battery");
                    }*/
                    if (isNFCAtRightEnable) {
                        rightBlockList.remove("nfc");
                    }
                    if (isHeadsetAtRightEnable) {
                        rightBlockList.remove("headset");
                    }
                    if (blockList != null) {
                        XposedHelpers.setStaticObjectField(MiuiEndIconManager, "RIGHT_BLOCK_LIST", rightBlockList);
                    } else {
                        mResHook.setObjectReplacement(lpparam.packageName, "array", "config_drip_right_block_statusBarIcons", rightBlockList.toArray(new String[0]));
                    }
                }
            });
        }

        if (isMoveLeft || isSwapWiFiAndMobileNetwork) {
            findAndHookConstructor(mStatusBarIconList, String[].class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    boolean isRightController = "StatusBarIconList".equals(param.thisObject.getClass().getSimpleName());
                    ArrayList<String> allStatusIcons = new ArrayList<>(Arrays.asList((String[]) param.args[0]));
                    if (isRightController) {
                        int startIndex = allStatusIcons.indexOf("mobile");
                        int endIndex = allStatusIcons.indexOf("demo_wifi") + 1;
                        List<String> removedIcons = allStatusIcons.subList(startIndex, endIndex);
                        removedIcons.clear();
                        if (!isMoveLeft) {
                            startIndex = allStatusIcons.indexOf("hd");
                            allStatusIcons.addAll(startIndex + 1, mSignalRelatedIcons);
                        }
                        param.args[0] = allStatusIcons.toArray(new String[0]);
                    } else if (isMoveLeft && !isSwapWiFiAndMobileNetwork) {
                        dripLeftIcons.addAll(allStatusIcons);
                        allStatusIcons.addAll(0, mSignalRelatedIcons);
                        param.args[0] = allStatusIcons.toArray(new String[0]);
                    }
                }
            });
        }

        if (isMoveLeft) {
            findAndHookMethod("com.android.systemui.statusbar.phone.MiuiStatusBarSignalPolicy", "initMiuiSlot", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    Object dripLeftController = XposedHelpers.callStaticMethod(findClass("com.android.systemui.Dependency", lpparam.classLoader), "get", findClass("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", lpparam.classLoader));
                    XposedHelpers.setObjectField(param.thisObject, "mIconController", dripLeftController);
                }
            });

            findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Object dripLeftController = XposedHelpers.callStaticMethod(findClass("com.android.systemui.Dependency", lpparam.classLoader), "get", findClass("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", lpparam.classLoader));
                    Object mDripIconManager = XposedHelpers.getObjectField(param.thisObject, "mDripLeftDarkIconManager");
                    ArrayList<String> blockList = new ArrayList<>();
                    int mCurrentStatusBarType = (int) XposedHelpers.getAdditionalInstanceField(dripLeftController, "mCurrentStatusBarType");
                    if (mCurrentStatusBarType != 1) {
                        blockList.addAll(dripLeftIcons);
                    }
                    XposedHelpers.callMethod(mDripIconManager, "setBlockList", blockList);
                    XposedHelpers.callMethod(dripLeftController, "refreshIconGroup", mDripIconManager);
                }
            });

            findAndHookMethod("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", "setStatusBarType", int.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    int mCurrentStatusBarType = XposedHelpers.getIntField(param.thisObject, "mCurrentStatusBarType");
                    Object dripLeftController = XposedHelpers.callStaticMethod(findClass("com.android.systemui.Dependency", lpparam.classLoader), "get", findClass("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl", lpparam.classLoader));
                    XposedHelpers.setAdditionalInstanceField(dripLeftController, "mCurrentStatusBarType", mCurrentStatusBarType);
                }
            });
        }

        if (isMoveLeft || isNetworkSpeedAtRightEnable) {
            hookAllMethods("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", "updateCutoutLocation", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    int mCurrentStatusBarType = (int) XposedHelpers.getObjectField(param.thisObject, "mCurrentStatusBarType");
                    if (mCurrentStatusBarType == 1) {
                        if (isNetworkSpeedAtRightEnable) {
                            Object mDripNetworkSpeedView = XposedHelpers.getObjectField(param.thisObject, "mDripNetworkSpeedView");
                            XposedHelpers.callMethod(mDripNetworkSpeedView, "setBlocked", true);
                        }
                    } else {
                        boolean dualRows = false;
                        if (isMoveLeft && !dualRows) {
                            View mDripStatusBarLeftStatusIconArea = (View) XposedHelpers.getObjectField(param.thisObject, "mDripStatusBarLeftStatusIconArea");
                            mDripStatusBarLeftStatusIconArea.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }

        if (isNetworkSpeedAtRightEnable) {
            hookAllMethods("com.android.systemui.statusbar.policy.NetworkSpeedController", "setDripNetworkSpeedView", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.args[0] = null;
                }
            });
        }
    }
}
