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
package com.sevtinge.hyperceiler.module.hook.home.navigation;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

import android.content.res.Configuration;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HideNavigationBar extends BaseHook {
    @Override
    public void init() {
        
        findAndHookMethod("com.miui.home.recents.views.RecentsContainer", "showLandscapeOverviewGestureView", boolean.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });

        findAndHookMethod("com.miui.home.recents.NavStubView", "isMistakeTouch", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                View navView = (View) param.thisObject;
                boolean setting = Settings.Global.getInt(navView.getContext().getContentResolver(), "show_mistake_touch_toast", 1) == 1;
                boolean misTouch = (boolean) callMethod(param.thisObject, "isLandScapeActually");
                param.setResult(misTouch && setting);
            }
        });

        findAndHookMethod("com.miui.home.recents.NavStubView", "onPointerEvent", MotionEvent.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                boolean mIsInFsMode = getBooleanField(param.thisObject, "mIsInFsMode");
                MotionEvent motionEvent = (MotionEvent) param.args[0];
                if (!mIsInFsMode) {
                    if (motionEvent.getAction() == 0) {
                        setBooleanField(param.thisObject, "mHideGestureLine", true);
                    }
                }
            }
        });

        findAndHookMethod("com.miui.home.recents.NavStubView", "updateScreenSize", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                setBooleanField(param.thisObject, "mHideGestureLine", false);
            }
        });

        findAndHookMethod("com.miui.home.recents.NavStubView", "onConfigurationChanged", Configuration.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                setBooleanField(param.thisObject, "mHideGestureLine", false);
            }
        });

    }
}
