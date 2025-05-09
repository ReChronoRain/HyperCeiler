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
package com.sevtinge.hyperceiler.hook.module.hook.home.navigation;

import android.view.WindowManager;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BackGestureAreaHeight extends BaseHook {
    @Override
    public void init() {
        try {   // 适用于5.39.10929+
            findAndHookMethod("com.miui.home.recents.GestureStubView", "updateGestureTouchHeight", new replaceHookedMethod() {
                @Override
                protected Object replace(MethodHookParam param) throws Throwable {
                    Object obj = param.thisObject;

                    int mRotation = XposedHelpers.getIntField(obj, "mRotation");
                    int mScreenHeight = XposedHelpers.getIntField(obj, "mScreenHeight");
                    int mScreenWidth = XposedHelpers.getIntField(obj, "mScreenWidth");

                    float f = (float) mPrefsMap.getInt("home_navigation_back_area_height", 60) / 100;

                    int gestureTouchHeight;
                    if (mRotation == 0 || mRotation == 2) {
                        gestureTouchHeight = (int) (mScreenHeight * f);
                    } else {
                        gestureTouchHeight = (int) (mScreenWidth * f);
                    }
                    XposedHelpers.setIntField(obj, "mGestureTouchHeight", gestureTouchHeight);

                    return null;
                }
            });
        } catch (NoSuchMethodError e) { // 旧版
            findAndHookMethodSilently("com.miui.home.recents.GestureStubView", "getGestureStubWindowParam", new MethodHook() {
                @Override
                protected void after(final MethodHookParam param) throws Throwable {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                    int pct = mPrefsMap.getInt("home_navigation_back_area_height", 60);  // 记得改key
                    lp.height = Math.round(lp.height / 60.0f * pct);
                    param.setResult(lp);
                }
            });
        }
    }
}
