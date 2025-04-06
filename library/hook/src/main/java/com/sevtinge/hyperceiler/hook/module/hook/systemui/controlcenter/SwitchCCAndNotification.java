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

import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class SwitchCCAndNotification extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod(
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView", lpparam.classLoader,
                "handleEvent",
                MotionEvent.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        boolean useCC = (boolean) XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mPanelController"), "isExpandable");
                        if (useCC) {
                            FrameLayout bar = (FrameLayout) param.thisObject;
                            Object mControlPanelWindowManager = XposedHelpers.getObjectField(param.thisObject, "mControlPanelWindowManager");
                            boolean dispatchToControlPanel = (boolean) XposedHelpers.callMethod(mControlPanelWindowManager, "dispatchToControlPanel", param.args[0], bar.getWidth());
                            XposedHelpers.setObjectField(mControlPanelWindowManager, "transToControlPanel", dispatchToControlPanel);
                            param.setResult(dispatchToControlPanel);
                            return;
                        }
                        param.setResult(false);
                    }
                });

        findAndHookMethod(
                "com.android.systemui.controlcenter.phone.ControlPanelWindowManager", lpparam.classLoader,
                "dispatchToControlPanel",
                MotionEvent.class, float.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        boolean added = XposedHelpers.getBooleanField(param.thisObject, "added");
                        if (added) {
                            boolean useCC;
                            Object controlCenterWindowView;

                            useCC = XposedHelpers.getBooleanField(XposedHelpers.getObjectField(param.thisObject, "controlCenterController"), "useControlCenter");
                            if (useCC) {
                                MotionEvent motionEvent = (MotionEvent) param.args[0];
                                if (motionEvent.getActionMasked() == 0) {
                                    XposedHelpers.setObjectField(param.thisObject, "downX", motionEvent.getRawX());
                                }

                                controlCenterWindowView = XposedHelpers.getObjectField(param.thisObject, "windowView");

                                if (controlCenterWindowView == null) {
                                    param.setResult(false);
                                } else {
                                    float mDownX;
                                    mDownX = XposedHelpers.getFloatField(param.thisObject, "downX");
                                    float width = (float) param.args[1];
                                    if (mDownX < width / 2.0f) {
                                        param.setResult(XposedHelpers.callMethod(controlCenterWindowView, "handleMotionEvent", motionEvent, true));
                                    } else {
                                        param.setResult(false);
                                    }
                                }
                                return;
                            }
                        }
                        param.setResult(false);
                    }
                });
    }
}
