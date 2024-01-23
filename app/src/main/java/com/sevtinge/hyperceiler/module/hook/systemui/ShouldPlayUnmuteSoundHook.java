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
package com.sevtinge.hyperceiler.module.hook.systemui;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class ShouldPlayUnmuteSoundHook extends BaseHook {

    Class<?> mQuietModeTile = XposedHelpers.findClassIfExists("com.android.systemui.qs.tiles.QuietModeTile", lpparam.classLoader);
    Class<?> mZenModeController = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.policy.ZenModeController", lpparam.classLoader);

    @Override
    public void init() {
        findAndHookMethod(mQuietModeTile, "handleClick", View.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(mZenModeController, "isZenModeOn", true);
            }
        });
    }
}
