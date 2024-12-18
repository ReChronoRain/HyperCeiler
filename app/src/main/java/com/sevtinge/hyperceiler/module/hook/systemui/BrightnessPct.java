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
package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.initPct;
import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.mPct;
import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.removePct;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class BrightnessPct extends BaseHook {
    @Override
    @SuppressLint("SetTextI18n")
    public void init() throws NoSuchMethodException {
        if (!isMoreHyperOSVersion(1f)) {
            findAndHookMethod("com.android.systemui.statusbar.policy.BrightnessMirrorController", "showMirror", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    ViewGroup mStatusBarWindow = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStatusBarWindow");
                    if (mStatusBarWindow == null) {
                        logE(TAG, lpparam.packageName, "mStatusBarWindow is null");
                        return;
                    }
                    initPct(mStatusBarWindow, 1);
                    mPct.setVisibility(View.VISIBLE);
                }
            });

            findAndHookMethod("com.android.systemui.statusbar.policy.BrightnessMirrorController", "hideMirror", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        removePct(mPct);
                    }
                }
            );

            hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", "onStart", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Object windowView = getObject(param);
                    if (windowView == null) {
                        logE(TAG, lpparam.packageName, "mControlPanelContentView is null");
                        return;
                    }
                    initPct((ViewGroup) windowView, 2);
                    mPct.setVisibility(View.VISIBLE);
                }
            });
        }

        hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", "onStop", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                removePct(mPct);
            }
        });

        final Class<?> brightnessUtils = findClassIfExists("com.android.systemui.controlcenter.policy.BrightnessUtils");
        hookAllMethods("com.android.systemui.controlcenter.policy.MiuiBrightnessController", "onChanged", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                int pctTag = 0;
                if (mPct != null && mPct.getTag() != null) {
                    pctTag = (int) mPct.getTag();
                }
                if (pctTag == 0 || mPct == null) return;
                int currentLevel = (int) param.args[3];
                if (brightnessUtils != null) {
                    int maxLevel = (int) XposedHelpers.getStaticObjectField(brightnessUtils, "GAMMA_SPACE_MAX");
                    mPct.setText(((currentLevel * 100) / maxLevel) + "%");
                }
            }
        });
    }

    private static Object getObject(XC_MethodHook.MethodHookParam param) {
        Object mMirror = XposedHelpers.getObjectField(param.thisObject, "mControl");
        Object controlCenterWindowViewController = XposedHelpers.getObjectField(mMirror, "controlCenterWindowViewController");
        String clsName = controlCenterWindowViewController.getClass().getName();
        if (!clsName.equals("ControlCenterWindowViewController")) {
            controlCenterWindowViewController = XposedHelpers.callMethod(controlCenterWindowViewController, "get");
        }
        return XposedHelpers.callMethod(controlCenterWindowViewController, "getView");
    }
}
