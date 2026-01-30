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

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getBooleanField;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setBooleanField;

import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class HideNavigationBar extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.views.RecentsContainer", "showLandscapeOverviewGestureView", boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(null);
                }
            });

        findAndHookMethod("com.miui.home.recents.NavStubView", "isMistakeTouch", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                View navView = (View) param.getThisObject();
                boolean setting = Settings.Global.getInt(navView.getContext().getContentResolver(), "show_mistake_touch_toast", 1) == 1;
                boolean misTouch = (boolean) callMethod(param.getThisObject(), "isLandScapeActually");
                param.setResult(misTouch && setting);
            }
        });

        findAndHookMethod("com.miui.home.recents.NavStubView", "onPointerEvent", MotionEvent.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                boolean mIsInFsMode = getBooleanField(param.getThisObject(), "mIsInFsMode");
                MotionEvent motionEvent = (MotionEvent) param.getArgs()[0];
                if (!mIsInFsMode) {
                    if (motionEvent.getAction() == 0) {
                        setBooleanField(param.getThisObject(), "mHideGestureLine", true);
                    }
                }
            }
        });

        findAndHookMethod("com.miui.home.recents.NavStubView", "updateScreenSize", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                setBooleanField(param.getThisObject(), "mHideGestureLine", false);
            }
        });

    }
}
