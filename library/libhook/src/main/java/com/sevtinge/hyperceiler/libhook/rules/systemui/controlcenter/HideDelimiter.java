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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;

import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class HideDelimiter extends BaseHook {

    boolean operator = mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 1;
    int prefs = mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0);
    String[] deviceNameList = {
        getProp("persist.sys.device_name")
    };

    @Override
    public void init() {
        try {
            findClass("com.android.systemui.statusbar.policy.MiuiCarrierTextController").getDeclaredMethod("fireCarrierTextChanged", String.class);
            findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController",
                    "fireCarrierTextChanged", String.class,
                    new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            String mCurrentCarrier = (String) param.getArgs()[0];
                            switch (prefs) {
                                case 1 -> param.getArgs()[0] = mCurrentCarrier.replace(" | ", "");
                                case 2 -> param.getArgs()[0] = "";
                                case 3 -> param.getArgs()[0] = getProp("persist.sys.device_name");
                            }
                        }
                    }
            );
        } catch (Throwable e) {
            try {
                findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl",
                        "addCallback", "com.android.systemui.plugins.miui.statusbar.MiuiCarrierTextController$CarrierTextListener",
                        new IMethodHook() {
                            @Override
                            public void before(BeforeHookParam param) {
                                String mCurrentCarrier = (String) getObjectField(param.getThisObject(), "mCurrentCarrier");
                                switch (prefs) {
                                    case 1 -> mCurrentCarrier = mCurrentCarrier.replace(" | ", "");
                                    case 2 -> mCurrentCarrier = "";
                                    case 3 -> mCurrentCarrier = getProp("persist.sys.device_name");
                                }
                                setObjectField(param.getThisObject(), "mCurrentCarrier", mCurrentCarrier);
                            }
                        }
                );
            } catch (Throwable ignore) {
                findAndHookMethod("com.android.keyguard.CarrierText",
                        "updateHDDrawable", int.class,
                        new IMethodHook() {
                            @Override
                            public void before(BeforeHookParam param) {
                                param.setResult(null);
                            }
                        }
                );

                findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController",
                        "fireCarrierTextChanged", int.class, int.class, String.class,
                        new IMethodHook() {
                            @Override
                            public void before(BeforeHookParam param) {
                                switch (prefs) {
                                    case 2 -> param.getArgs()[2] = "";
                                    case 3 -> param.getArgs()[2] = getProp("persist.sys.device_name");
                                }
                            }
                        }
                );

                if (operator) {
                    // 暂时不懂分隔线怎么去掉
                    findAndHookMethod("com.android.keyguard.CarrierText$1",
                            "onCarrierTextChanged", int.class, int.class, String.class,
                            new IMethodHook() {
                                @Override
                                public void before(BeforeHookParam param) {
                                    String mCurrentCarrier = (String) param.getArgs()[2];
                                    param.getArgs()[2] = mCurrentCarrier.replace(" | ", "");
                                }
                            }
                    );
                }
            }

            if (prefs == 3) {

                if (!isMoreSmallVersion(200, 2f)) {
                    findAndHookMethod("miui.stub.statusbar.StatusBarStub$registerMiuiCarrierTextController$1$addCallback$callback$1", "onCarrierTextChanged", String.class, new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.getArgs()[0] = getProp("persist.sys.device_name");
                        }
                    });

                    findAndHookMethod("miui.stub.statusbar.StatusBarStub$registerMiuiCarrierTextController$1$addCallback$callback$1", "onCarrierTextChanged", String.class, int.class, new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.getArgs()[0] = getProp("persist.sys.device_name");
                            param.getArgs()[1] = 1;
                        }
                    });

                    findAndHookMethod("com.android.keyguard.CarrierText$$ExternalSyntheticLambda0", "onCarrierTextChanged", String.class, new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.getArgs()[0] = getProp("persist.sys.device_name");
                        }
                    });

                } else {
                    findAndHookMethod("miui.stub.statusbar.StatusBarStub$registerMiuiCarrierTextController$1$addCallback$callback$1", "onCarrierTextChanged", int.class, String.class, new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.getArgs()[0] = 1;
                            param.getArgs()[1] = getProp("persist.sys.device_name");
                        }
                    });

                    findAndHookMethod("com.android.keyguard.CarrierText$1", "onCarrierTextChanged", int.class, int.class, String.class, new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.getArgs()[0] = 1;
                            param.getArgs()[2] = getProp("persist.sys.device_name");
                        }
                    });
                }
                try {
                    findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl", "updateCarrierText", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            setObjectField(param.getThisObject(), "mCurrentCarrier", getProp("persist.sys.device_name"));
                            setObjectField(param.getThisObject(), "mCustomCarrier", deviceNameList);
                            setObjectField(param.getThisObject(), "mCarrier", deviceNameList);
                            setObjectField(param.getThisObject(), "mRealCarrier", deviceNameList);
                        }
                    });
                } catch(Throwable ignore) {
                    findAndHookMethod("com.android.systemui.statusbar.policy.MiuiCarrierTextController", "updateCarrierText", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            setObjectField(param.getThisObject(), "mCurrentCarrier", getProp("persist.sys.device_name"));
                            setObjectField(param.getThisObject(), "mCustomCarrier", deviceNameList);
                            setObjectField(param.getThisObject(), "mCarrier", deviceNameList);
                            setObjectField(param.getThisObject(), "mRealCarrier", deviceNameList);
                        }
                    });
                }


                findAndHookMethod(SubscriptionInfo.class, "getCarrierName", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.setResult(getProp("persist.sys.device_name"));
                    }
                });

            } else {
                findAndHookMethod("androidx.constraintlayout.core.PriorityGoalRow$GoalVariableAccessor$$ExternalSyntheticOutline0",
                        "m", String.class, String.class, new IMethodHook() {
                            @Override
                            public void before(BeforeHookParam param) {
                                // param.getArgs()[0] = getProp("persist.sys.device_name");
                                if (param.getArgs()[1].equals(" | ")) {
                                    param.getArgs()[1] = "";
                                }
                            }
                        }
                );

                findAndHookMethod("androidx.concurrent.futures.AbstractResolvableFuture$$ExternalSyntheticOutline0",
                        "m", String.class, String.class, String.class,
                        new IMethodHook() {
                            @Override
                            public void before(BeforeHookParam param) {
                                // param.getArgs()[0] = getProp("persist.sys.device_name");
                                if (param.getArgs()[1].equals(" | ")) {
                                    param.getArgs()[1] = "";
                                }
                            }
                        }
                );
            }
        }

        if (prefs == 2) {
            IMethodHook hideOperatorHook = new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Object carrierView;
                    TextView mCarrierText;
                    try {
                        carrierView = getObjectField(param.getThisObject(), "carrierText");
                    } catch (Throwable e) {
                        carrierView = getObjectField(param.getThisObject(), "mCarrierText");
                    }
                    mCarrierText = (TextView) carrierView;
                    mCarrierText.setVisibility(View.GONE);
                }
            };

            boolean hookedFlaresInfo;
            try {
                hookAllMethods("com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar",
                    "updateFlaresInfo", hideOperatorHook);
                hookedFlaresInfo = true;
            } catch (Throwable ingore) {
                hookedFlaresInfo = false;
            }
            if (!hookedFlaresInfo) {
                findAndHookMethod("com.android.systemui.controlcenter.phone.widget.ControlCenterStatusBar",
                        "onFinishInflate", hideOperatorHook);
            }

            findAndHookMethod("com.android.systemui.qs.MiuiNotificationHeaderView",
                    "updateCarrierTextVisibility", hideOperatorHook);

            try {
                findAndHookMethod("com.android.systemui.qs.MiuiQSHeaderView",
                    "updateCarrierVisibility", hideOperatorHook);
                hookedFlaresInfo = true;
            } catch (Throwable ingore) {
                hookedFlaresInfo = false;
            }
            if (!hookedFlaresInfo) {
                findAndHookMethod("com.android.systemui.qs.MiuiQSHeaderView",
                        "onFinishInflate", hideOperatorHook);
            }
        }
    }
}

