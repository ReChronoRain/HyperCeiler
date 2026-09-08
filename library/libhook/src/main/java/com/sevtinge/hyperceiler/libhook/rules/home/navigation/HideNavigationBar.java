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
package com.sevtinge.hyperceiler.libhook.rules.home.navigation;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getBooleanField;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import android.graphics.Canvas;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class HideNavigationBar extends BaseHook {
    @Override
    public void init() {
        if (isMoreAndroidVersion(36) && isMoreHyperOSVersion(3.0f)) {
            // >= Android 16 & >= HyperOS 3.0+: 使用 onDraw Hook 隐藏方案
            // 尝试从 systemui 主进程查找类
            Class<?> bottomViewClass = findClassIfExists("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationBottomView");
            // 尝试从 systemui 插件中查找类
            if (bottomViewClass == null) {
                bottomViewClass = findClassIfExists("miui.systemui.plugin.MiuiDecorationBottomView");
            }
            if (bottomViewClass == null) {
                XposedLog.w(TAG, getPackageName(), "MiuiDecorationBottomView class not found");
                return;
            }
            XposedLog.d(TAG, "bottomViewClass founded");
            try {
                findAndHookMethod(bottomViewClass, "onDraw", Canvas.class, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        try {
                            // 跳过原始绘制逻辑，隐藏手势提示线
                            param.setResult(null);
                            XposedLog.d(TAG, "bottomViewClass hide done");
                        } catch (Throwable t) {
                            XposedLog.w(TAG, getPackageName(), "HideNavigationBar before error", t);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedLog.e(TAG, getPackageName(), "Hook MiuiDecorationBottomView.onDraw failed", t);
            }
        } else {
            // < Android 16 或 < HyperOS 3.0+: 使用旧的资源替换方案
            findAndHookMethod("com.miui.home.recents.views.RecentsContainer", "showLandscapeOverviewGestureView", boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(null);
                    }
                });

            findAndHookMethod("com.miui.home.recents.NavStubView", "isMistakeTouch", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    View navView = (View) param.getThisObject();
                    boolean setting = Settings.Global.getInt(navView.getContext().getContentResolver(), "show_mistake_touch_toast", 1) == 1;
                    boolean misTouch = (boolean) callMethod(param.getThisObject(), "isLandScapeActually");
                    param.setResult(misTouch && setting);
                }
            });

            findAndHookMethod("com.miui.home.recents.NavStubView", "onPointerEvent", MotionEvent.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    boolean mIsInFsMode = getBooleanField(param.getThisObject(), "mIsInFsMode");
                    MotionEvent motionEvent = (MotionEvent) param.getArgs()[0];
                    if (!mIsInFsMode && motionEvent != null && motionEvent.getAction() == 0) {
                        setBooleanField(param.getThisObject(), "mHideGestureLine", true);
                    }
                }
            });

            findAndHookMethod("com.miui.home.recents.NavStubView", "updateScreenSize", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    setBooleanField(param.getThisObject(), "mHideGestureLine", false);
                }
            });
        }

    }
}
