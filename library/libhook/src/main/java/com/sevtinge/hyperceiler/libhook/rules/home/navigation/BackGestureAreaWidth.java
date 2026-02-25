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

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class BackGestureAreaWidth extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.GestureStubView", "initScreenSizeAndDensity", int.class, new IMethodHook() {
            @Override
            public void after(final AfterHookParam param) {
                int pct = PrefsBridge.getInt("home_navigation_back_area_width", 100);
                if (pct == 100) return;
                int mGestureStubDefaultSize = EzxHelpUtils.getIntField(param.getThisObject(), "mGestureStubDefaultSize");
                int mGestureStubSize  = EzxHelpUtils.getIntField(param.getThisObject(), "mGestureStubSize");
                mGestureStubDefaultSize = Math.round(mGestureStubDefaultSize * pct / 100f);
                mGestureStubSize = Math.round(mGestureStubSize * pct / 100f);
                EzxHelpUtils.setIntField(param.getThisObject(), "mGestureStubDefaultSize", mGestureStubDefaultSize);
                EzxHelpUtils.setIntField(param.getThisObject(), "mGestureStubSize", mGestureStubSize);
            }
        });

        findAndHookMethod("com.miui.home.recents.GestureStubView", "setSize", int.class, new IMethodHook() {
            @Override
            public void before(final BeforeHookParam param) {
                int pct = PrefsBridge.getInt("home_navigation_back_area_width", 100);
                if (pct == 100) return;
                int mGestureStubDefaultSize = EzxHelpUtils.getIntField(param.getThisObject(), "mGestureStubDefaultSize");
                if ((int)param.getArgs()[0] == mGestureStubDefaultSize) return;
                param.getArgs()[0] = Math.round((int)param.getArgs()[0] * pct / 100f);
            }
        });
    }
}
