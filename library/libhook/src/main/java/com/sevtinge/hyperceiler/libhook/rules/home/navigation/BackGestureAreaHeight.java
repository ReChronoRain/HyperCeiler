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

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.view.WindowManager;

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.callback.IReplaceHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class BackGestureAreaHeight extends HomeBaseHookNew {

    @Version(isPad = true)
    private void initPadHook() {
        findAndHookMethod("com.miui.home.recents.GestureStubView", "getGestureStubWindowParam", new IMethodHook() {
            @Override
            public void after(final AfterHookParam param) {
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                int pct = mPrefsMap.getInt("home_navigation_back_area_height", 60);
                lp.height = Math.round(lp.height / 100.0f * pct);
                lp.width = Math.round(lp.width / 100.0f * pct);
                param.setResult(lp);
            }
        });
    }

    @Override
    public void initBase() {
        try {   // 适用于5.39.10929+
            findAndReplaceMethod("com.miui.home.recents.GestureStubView", "updateGestureTouchHeight", new IReplaceHook() {
                @Override
                public Object replace(BeforeHookParam param) throws Throwable {
                    Object obj = param.getThisObject();

                    int mRotation = EzxHelpUtils.getIntField(obj, "mRotation");
                    int mScreenHeight = EzxHelpUtils.getIntField(obj, "mScreenHeight");
                    int mScreenWidth = EzxHelpUtils.getIntField(obj, "mScreenWidth");

                    float f = (float) mPrefsMap.getInt("home_navigation_back_area_height", 60) / 100;

                    int gestureTouchHeight;
                    if (mRotation == 0 || mRotation == 2) {
                        gestureTouchHeight = (int) (mScreenHeight * f);
                    } else {
                        gestureTouchHeight = (int) (mScreenWidth * f);
                    }
                    EzxHelpUtils.setIntField(obj, "mGestureTouchHeight", gestureTouchHeight);

                    return null;
                }
            });
        } catch (NoSuchMethodError e) { // 旧版
            findAndHookMethod("com.miui.home.recents.GestureStubView", "getGestureStubWindowParam", new IMethodHook() {
                @Override
                public void after(final AfterHookParam param) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                    int pct = mPrefsMap.getInt("home_navigation_back_area_height", 60);
                    if (isPad()) {
                        lp.height = Math.round(lp.height / 100.0f * pct);
                        lp.width = Math.round(lp.width / 100.0f * pct);
                    } else {
                        lp.height = Math.round(lp.height / 60.0f * pct);
                    }
                    param.setResult(lp);
                }
            });
        }
    }
}
