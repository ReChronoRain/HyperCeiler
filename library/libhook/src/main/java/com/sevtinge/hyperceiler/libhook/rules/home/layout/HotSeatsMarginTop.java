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
package com.sevtinge.hyperceiler.libhook.rules.home.layout;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class HotSeatsMarginTop extends HomeBaseHookNew {

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        findAndHookMethod(DEVICE_CONFIG_NEW, "calcHotSeatsMarginTop", Context.class, boolean.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                Context context = (Context) param.getArgs()[0];
                param.setResult(DisplayUtils.dp2px(context, PrefsBridge.getInt("home_layout_hotseats_margin_top", 60)));
            }
        });
    }

    @Override
    public void initBase() {
        findAndHookMethod(DEVICE_CONFIG_OLD, "calcHotSeatsMarginTop", Context.class, boolean.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                Context context = (Context) param.getArgs()[0];
                param.setResult(DisplayUtils.dp2px(context, PrefsBridge.getInt("home_layout_hotseats_margin_top", 60)));
            }
        });
    }
}
