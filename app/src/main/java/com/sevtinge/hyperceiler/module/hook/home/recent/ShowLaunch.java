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
package com.sevtinge.hyperceiler.module.hook.home.recent;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class ShowLaunch extends BaseHook {
    //from XiaomiHelper by HowieHChen
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.recents.NavStubView", "changeAlphaScaleForFsGesture", float.class, float.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = (1.0f - (float) mPrefsMap.getInt("home_recent_show_launch_alpha", 100) / 100) * (float) param.args[0] + (float) mPrefsMap.getInt("home_recent_show_launch_alpha", 100) / 100;
            }
        });
        findAndHookMethod("com.miui.home.recents.OverviewState", "getShortcutMenuLayerAlpha", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult((1.0f - (float) mPrefsMap.getInt("home_recent_show_launch_alpha", 100) / 100) * (float) param.getResult() + (float) mPrefsMap.getInt("home_recent_show_launch_alpha", 100) / 100);
            }
        });
        findAndHookMethod("com.miui.home.recents.OverviewState", "getShortcutMenuLayerScale", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult((float) mPrefsMap.getInt("home_recent_show_launch_size", 95) / 100);
            }
        });
    }
}
