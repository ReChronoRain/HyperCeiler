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
package com.sevtinge.hyperceiler.libhook.rules.home;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class WidgetCornerRadius extends BaseHook {

    Context mContext;

    @Override
    public void init() {

        hookAllConstructors("com.miui.home.launcher.maml.MaMlHostView", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                mContext = (Context) param.getArgs()[0];
            }
        });

        hookAllMethods("com.miui.home.launcher.maml.MaMlHostView", "computeRoundedCornerRadius", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult((float) DisplayUtils.dp2px(PrefsBridge.getInt("home_widget_corner_radius", 0)));
            }
        });


        hookAllConstructors("com.miui.home.launcher.LauncherAppWidgetHostView", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                mContext = (Context) param.getArgs()[0];
            }
        });

        hookAllMethods("com.miui.home.launcher.LauncherAppWidgetHostView", "computeRoundedCornerRadius", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.setResult((float) DisplayUtils.dp2px(PrefsBridge.getInt("home_widget_corner_radius", 0)));
            }
        });
    }
}
