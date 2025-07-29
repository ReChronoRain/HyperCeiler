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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.hook.utils.PropUtils.getProp;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class HideDelimiter extends BaseHook {

    boolean operator = mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 1;
    int prefs = mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0);
    String[] deviceNameList = {getProp("persist.sys.device_name")};

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
                                case 3 -> param.args[0] = getProp("persist.sys.device_name");
                            }
                        }
                    }
            );
        } catch (Throwable e) {
            try {
                findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl",
                        "addCallback", "com.android.systemui.plugins.miui.statusbar.MiuiCarrierTextController$CarrierTextListener",
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                String mCurrentCarrier = (String) XposedHelpers.getObjectField(param.thisObject, "mCurrentCarrier");
                                switch (prefs) {
                                    case 1 -> mCurrentCarrier = mCurrentCarrier.replace(" | ", "");
                                    case 2 -> mCurrentCarrier = "";
                                    case 3 -> mCurrentCarrier = getProp("persist.sys.device_name");
                                }
                                XposedHelpers.setObjectField(param.thisObject, "mCurrentCarrier", mCurrentCarrier);
                            }
                        }
                );
            } catch (Throwable ignore) {
                findAndHookMethod("com.android.keyguard.CarrierText",
                        "updateHDDrawable", int.class,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                param.setResult(null);
                            }
                        }
                );

                findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController",
                        "fireCarrierTextChanged", int.class, int.class, String.class,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                switch (prefs) {
                                    case 2 -> param.args[2] = "";
                                    case 3 -> param.args[2] = getProp("persist.sys.device_name");
                                }
                            }
                        }
                );

                if (operator) {
                    // 暂时不懂分隔线怎么去掉
                    findAndHookMethod("com.android.keyguard.CarrierText$1",
                            "onCarrierTextChanged", int.class, int.class, String.class,
                            new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) {
                                    String mCurrentCarrier = (String) param.args[2];
                                    param.args[2] = mCurrentCarrier.replace(" | ", "");
                                }
                            }
                    );
                }
            }

            if (prefs == 3) {
                if (isMoreAndroidVersion(35)) {
                    try {
                        findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl", "updateCarrierText", new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                XposedHelpers.setObjectField(param.thisObject, "mCurrentCarrier", getProp("persist.sys.device_name"));
                                XposedHelpers.setObjectField(param.thisObject, "mCustomCarrier", deviceNameList);
                                XposedHelpers.setObjectField(param.thisObject, "mCarrier", deviceNameList);
                                XposedHelpers.setObjectField(param.thisObject, "mRealCarrier", deviceNameList);
                            }
                        });
                    } catch(Throwable ignore) {
                        findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController", "updateCarrierText", new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                XposedHelpers.setObjectField(param.thisObject, "mCurrentCarrier", getProp("persist.sys.device_name"));
                                XposedHelpers.setObjectField(param.thisObject, "mCustomCarrier", deviceNameList);
                                XposedHelpers.setObjectField(param.thisObject, "mCarrier", deviceNameList);
                                XposedHelpers.setObjectField(param.thisObject, "mRealCarrier", deviceNameList);
                            }
                        });
                    }


                    findAndHookMethod(SubscriptionInfo.class, "getCarrierName", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.setResult(getProp("persist.sys.device_name"));
                        }
                    });

                } else {
                    findAndHookMethod("com.android.keyguard.clock.KeyguardClockContainer$mCarrierTextCallback$1", "onCarrierTextChanged", String.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.args[0] = getProp("persist.sys.device_name");
                        }
                    });

                    findAndHookMethod("com.android.keyguard.clock.KeyguardClockContainer$mCarrierTextCallback$1", "onCarrierTextChanged", String.class, int.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.args[0] = getProp("persist.sys.device_name");
                            param.args[1] = 1;
                        }
                    });

                    findAndHookMethod("com.android.keyguard.CarrierText$1", "onCarrierTextChanged", String.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.args[0] = getProp("persist.sys.device_name");
                        }
                    });

                    findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl", "updateCarrierText", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            XposedHelpers.setObjectField(param.thisObject, "mCurrentCarrier", getProp("persist.sys.device_name"));
                            XposedHelpers.setObjectField(param.thisObject, "mCustomCarrier", deviceNameList);
                            XposedHelpers.setObjectField(param.thisObject, "mCarrier", deviceNameList);
                            XposedHelpers.setObjectField(param.thisObject, "mRealCarrier", deviceNameList);
                        }
                    });

                    findAndHookMethod(SubscriptionInfo.class, "getCarrierName", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.setResult(getProp("persist.sys.device_name"));
                        }
                    });

                    findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl", "onCarrierChanged", String[].class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            param.args[0] = deviceNameList;
                            XposedHelpers.setObjectField(param.thisObject, "mRealCarrier", deviceNameList);
                        }
                    });

                }

            } else {
                findAndHookMethod("androidx.constraintlayout.core.PriorityGoalRow$GoalVariableAccessor$$ExternalSyntheticOutline0",
                        "m", String.class, String.class, new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                super.before(param);
                                // param.args[0] = getProp("persist.sys.device_name");
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
                                // param.args[0] = getProp("persist.sys.device_name");
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

