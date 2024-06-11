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

import android.content.res.Configuration;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.module.base.BaseTool;

public class HideNavigationBar extends BaseTool {
    @Override
    public void doHook() {
        hcHook.findClass("rc", "com.miui.home.recents.views.RecentsContainer")
                .getMethod("showLandscapeOverviewGestureView", boolean.class)
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        param.first(false);
                    }
                });

        hcHook.findClass("nsv", "com.miui.home.recents.NavStubView")
                .getMethod("isMistakeTouch")
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        View navView = param.thisObject();
                        boolean setting = Settings.Global.getInt(navView.getContext().getContentResolver(), "show_mistake_touch_toast", 1) == 1;
                        boolean misTouch = param.callMethod("isLandScapeActually");
                        param.setResult(misTouch && setting);
                    }
                })
                .getMethod("onPointerEvent", MotionEvent.class)
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        boolean mIsInFsMode = param.getField("mIsInFsMode");
                        MotionEvent motionEvent = param.first();
                        if (!mIsInFsMode) {
                            if (motionEvent.getAction() == 0) {
                                param.setField("mHideGestureLine", true);
                            }
                        }
                    }
                })
                .getMethod("updateScreenSize")
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        param.setField("mHideGestureLine", false);
                    }
                })
                .getMethod("onConfigurationChanged", Configuration.class)
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        param.setField("mHideGestureLine", false);
                    }
                });
    }
}
