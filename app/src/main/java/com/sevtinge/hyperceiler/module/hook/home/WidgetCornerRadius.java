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
package com.sevtinge.hyperceiler.module.hook.home;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DisplayUtils;

public class WidgetCornerRadius extends BaseHook {

    Context mContext;

    @Override
    public void init() {

        hookAllConstructors("com.miui.home.launcher.maml.MaMlHostView", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mContext = (Context) param.args[0];
            }
        });

        hookAllMethods("com.miui.home.launcher.maml.MaMlHostView", "computeRoundedCornerRadius", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult((float) DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_widget_corner_radius", 0)));
            }
        });


        hookAllConstructors("com.miui.home.launcher.LauncherAppWidgetHostView", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mContext = (Context) param.args[0];
            }
        });

        hookAllMethods("com.miui.home.launcher.LauncherAppWidgetHostView", "computeRoundedCornerRadius", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult((float) DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_widget_corner_radius", 0)));
            }
        });
    }
}
