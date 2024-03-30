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
package com.sevtinge.hyperceiler.module.hook.home.navigation;

import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class HideNavigationBar extends BaseHook {
    @Override
    public void init() {
        /*横屏隐藏*/
        findAndHookMethod("com.miui.home.recents.views.RecentsContainer",
                "showLandscapeOverviewGestureView",
                boolean.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.args[0] = false;
                    }
                }
        );

        /*锁定返回*/
        findAndHookMethod("com.miui.home.recents.NavStubView",
                "isMistakeTouch",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        // boolean mIsShowStatusBar = (boolean) XposedHelpers.callMethod(param.thisObject, "isImmersive");
                        /*boolean mIsShowNavBar = XposedHelpers.getBooleanField(param.thisObject, "mIsShowNavBar");
                        boolean mHideGestureLine = XposedHelpers.getBooleanField(param.thisObject, "mHideGestureLine");
                        boolean mIsShowStatusBar = XposedHelpers.getBooleanField(param.thisObject, "mIsShowStatusBar");
                        logE(TAG, "mIsShowNavBar: " + mIsShowNavBar);
                        logE(TAG, "mHideGestureLine: " + mHideGestureLine);
                        logE(TAG, "mIsShowStatusBar: " + mIsShowStatusBar);*/
                        // 按道理仅横屏时显示也是可以的，不知道为什么小米要判断这么多。
                        View navView = (View) param.thisObject;
                        boolean setting = Settings.Global.getInt(navView.getContext().getContentResolver(), "show_mistake_touch_toast", 1) == 1;
                        boolean misTouch = (boolean) XposedHelpers.callMethod(param.thisObject, "isLandScapeActually");
                        param.setResult(misTouch && setting);
                    }
                }
        );

        /*横屏设置状态*/
        findAndHookMethod("com.miui.home.recents.NavStubView", "onPointerEvent",
                MotionEvent.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        boolean mIsInFsMode = XposedHelpers.getBooleanField(param.thisObject, "mIsInFsMode");
                        MotionEvent motionEvent = (MotionEvent) param.args[0];
                        if (!mIsInFsMode) {
                            if (motionEvent.getAction() == 0) {
                                XposedHelpers.setObjectField(param.thisObject, "mHideGestureLine", true);
                                // XposedHelpers.setObjectField(param.thisObject, "mIsShowNavBar", true);
                                // XposedHelpers.setObjectField(param.thisObject, "mIsShowStatusBar", true);
                            }
                        }
                    }
                }
        );

        /*恢复状态*/
        findAndHookMethod("com.miui.home.recents.NavStubView", "updateScreenSize",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        XposedHelpers.setObjectField(param.thisObject, "mHideGestureLine", false);
                        // XposedHelpers.setObjectField(param.thisObject, "mIsShowNavBar", true);
                        // XposedHelpers.setObjectField(param.thisObject, "mIsShowStatusBar", true);
                    }
                }
        );

    }
}
