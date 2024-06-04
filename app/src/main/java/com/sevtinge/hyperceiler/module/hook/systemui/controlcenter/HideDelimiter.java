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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.PropUtils.getProp;

import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class HideDelimiter extends BaseHook {

    boolean operator = mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 1;
    int prefs = mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0);
    String deviceName = getProp("persist.sys.device_name");
    String[] deviceNameList = {deviceName};

    @Override
    public void init() {
        try {
            findClass("com.android.systemui.statusbar.policy.MiuiCarrierTextController").getDeclaredMethod("fireCarrierTextChanged", String.class);
            findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController",
                    "fireCarrierTextChanged", String.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            String mCurrentCarrier = (String) param.args[0];
                            switch (prefs) {
                                case 1 -> param.args[0] = mCurrentCarrier.replace(" | ", "");
                                case 2 -> param.args[0] = "";
                                case 3 -> param.args[0] = deviceName;
                            }
                        }
                    }
            );
        } catch (Throwable e) {
            findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl",
                    "addCallback", "com.android.systemui.plugins.miui.statusbar.MiuiCarrierTextController$CarrierTextListener",
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            String mCurrentCarrier = (String) XposedHelpers.getObjectField(param.thisObject, "mCurrentCarrier");
                            switch (prefs) {
                                case 1 -> mCurrentCarrier = mCurrentCarrier.replace(" | ", "");
                                case 2 -> mCurrentCarrier = "";
                                case 3 -> mCurrentCarrier = deviceName;
                            }
                            XposedHelpers.setObjectField(param.thisObject, "mCurrentCarrier", mCurrentCarrier);
                        }
                    }
            );

            if (prefs == 3) {
                findAndHookMethod("com.android.keyguard.clock.KeyguardClockContainer$mCarrierTextCallback$1", "onCarrierTextChanged", String.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[0] = deviceName;
                    }
                });

                findAndHookMethod("com.android.keyguard.clock.KeyguardClockContainer$mCarrierTextCallback$1", "onCarrierTextChanged", String.class, int.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[0] = deviceName;
                    }
                });

                findAndHookMethod("com.android.keyguard.CarrierText$1", "onCarrierTextChanged", String.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[0] = deviceName;
                    }
                });

                findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl", "updateCarrierText", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        XposedHelpers.setObjectField(param.thisObject, "mCurrentCarrier", deviceName);
                        XposedHelpers.setObjectField(param.thisObject, "mCustomCarrier", deviceNameList);
                        XposedHelpers.setObjectField(param.thisObject, "mCarrier", deviceNameList);
                    }
                });

                findAndHookMethod(SubscriptionInfo.class, "getCarrierName", new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(deviceName);
                    }
                });

                findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl", "onCarrierChanged", String[].class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String[] deviceNameList = new String[deviceName.length()];
                        for (int i = 0; i < deviceName.length(); i++){
                            deviceNameList[i] = String.valueOf(deviceName.charAt(i));
                        }
                        param.args[0] = deviceNameList;
                        XposedHelpers.setObjectField(param.thisObject, "mRealCarrier", deviceNameList);
                    }
                });

            } else {
                findAndHookMethod("androidx.constraintlayout.core.PriorityGoalRow$GoalVariableAccessor$$ExternalSyntheticOutline0",
                        "m", String.class, String.class, new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                super.before(param);
                                // param.args[0] = deviceName;
                                if (param.args[1].equals(" | ")) {
                                    param.args[1] = "";
                                }
                            }
                        }
                );

                findAndHookMethodSilently("androidx.concurrent.futures.AbstractResolvableFuture$$ExternalSyntheticOutline0",
                        "m", String.class, String.class, String.class,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                // param.args[0] = deviceName;
                                if (param.args[1].equals(" | ")) {
                                    param.args[1] = "";
                                }
                            }
                        }
                );
            }
        }

        if (prefs == 2) {
            MethodHook hideOperatorHook = new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    super.after(param);
                    Object carrierView;
                    TextView mCarrierText;
                    try {
                        carrierView = XposedHelpers.getObjectField(param.thisObject, "carrierText");
                    } catch (Throwable e) {
                        carrierView = XposedHelpers.getObjectField(param.thisObject, "mCarrierText");
                    }
                    mCarrierText = (TextView) carrierView;
                    mCarrierText.setVisibility(View.GONE);
                }
            };

            boolean hookedFlaresInfo = hookAllMethodsBoolean("com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar",
                    "updateFlaresInfo", hideOperatorHook);
            if (!hookedFlaresInfo) {
                findAndHookMethodSilently("com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar",
                        "onFinishInflate", hideOperatorHook);
            }

            findAndHookMethodSilently("com.android.systemui.qs.MiuiNotificationHeaderView",
                    "updateCarrierTextVisibility", hideOperatorHook);

            hookedFlaresInfo = findAndHookMethodSilently("com.android.systemui.qs.MiuiQSHeaderView",
                    "updateCarrierVisibility", hideOperatorHook);
            if (!hookedFlaresInfo) {
                findAndHookMethodSilently("com.android.systemui.qs.MiuiQSHeaderView",
                        "onFinishInflate", hideOperatorHook);
            }
        }
    }
}

