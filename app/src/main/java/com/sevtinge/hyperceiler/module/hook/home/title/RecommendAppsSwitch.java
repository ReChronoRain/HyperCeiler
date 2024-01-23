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
package com.sevtinge.hyperceiler.module.hook.home.title;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class RecommendAppsSwitch extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.Folder",
            "showRecommendAppsSwitch", boolean.class, boolean.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    View mRecommendAppsSwitch = (View) XposedHelpers.getObjectField(param.thisObject,
                        "mRecommendAppsSwitch");
                    mRecommendAppsSwitch.setVisibility(View.GONE);
                }
            }
        );
    }
}
