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

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.callback.IAction;

public class HideNavigationBar extends BaseHC {
    @Override
    public void init() {
        
        hook("com.miui.home.recents.views.RecentsContainer", "showLandscapeOverviewGestureView", boolean.class,
                new IAction() {
                    @Override
                    public void before() throws Throwable {
                        first(false);
                    }
                });
        
        chain("com.miui.home.recents.NavStubView",method("isMistakeTouch")
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        View navView = thisObject();
                        boolean setting = Settings.Global.getInt(navView.getContext().getContentResolver(), "show_mistake_touch_toast", 1) == 1;
                        boolean misTouch = callThisMethod("isLandScapeActually");
                        setResult(misTouch && setting);
                    }
                })
                
                .method("onPointerEvent", MotionEvent.class)
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        boolean mIsInFsMode = getThisField("mIsInFsMode");
                        MotionEvent motionEvent = first();
                        if (!mIsInFsMode) {
                            if (motionEvent.getAction() == 0) {
                                setThisField("mHideGestureLine", true);
                            }
                        }
                    }
                })
                
                .method("updateScreenSize")
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        setThisField("mHideGestureLine", false);
                    }
                })
                
                .method("onConfigurationChanged", Configuration.class)
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        setThisField("mHideGestureLine", false);
                    }
                })
        );
    }
}
