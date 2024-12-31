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

import android.view.WindowManager;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class BackGestureAreaHeight extends BaseHook {
    @Override
    public void init() {
        findAndHookMethodSilently("com.miui.home.recents.GestureStubView",  "getGestureStubWindowParam", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams)param.getResult();
                int pct = mPrefsMap.getInt("home_navigation_back_area_height", 60);  //记得改key
                lp.height = Math.round(lp.height / 60.0f * pct);
                param.setResult(lp);
            }
        });
    }
}
