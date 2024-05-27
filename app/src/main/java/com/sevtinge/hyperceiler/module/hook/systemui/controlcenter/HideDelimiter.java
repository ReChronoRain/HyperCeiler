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

import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class HideDelimiter extends BaseHook {

    boolean operator = mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 1;

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
                            param.args[0] = operator ? mCurrentCarrier.replace(" | ", "") : "";
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
                            String updatedCarrier = mCurrentCarrier.replace(" | ", "");
                            XposedHelpers.setObjectField(param.thisObject, "mCurrentCarrier", operator ? updatedCarrier : "");
                        }
                    }
            );

            findAndHookMethod("androidx.constraintlayout.core.PriorityGoalRow$GoalVariableAccessor$$ExternalSyntheticOutline0",
                    "m", String.class, String.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            super.before(param);
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
                            if (param.args[1].equals(" | ")) {
                                param.args[1] = "";
                            }
                        }
                    }
            );
        }

        if (!operator) {
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
