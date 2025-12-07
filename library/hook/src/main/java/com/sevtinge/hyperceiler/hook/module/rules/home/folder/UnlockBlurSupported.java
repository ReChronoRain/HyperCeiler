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
package com.sevtinge.hyperceiler.hook.module.rules.home.folder;

import android.graphics.Rect;
import android.view.View;

import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew;

import de.robv.android.xposed.XposedHelpers;

public class UnlockBlurSupported extends HomeBaseHookNew {

    Class<?> mDeviceConfig;
    String mBlurUtilitiesCls;
    String mLauncherFolder2x2IconContainer;

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        mDeviceConfig = findClassIfExists(DEVICE_CONFIG_NEW);
        mBlurUtilitiesCls = "com.miui.home.common.utils.BlurUtilities";
        mLauncherFolder2x2IconContainer = "com.miui.home.folder.LauncherFolder2x2IconContainer";
        initBaseCore();
    }

    @Override
    public void initBase() {
        mDeviceConfig = findClassIfExists(DEVICE_CONFIG_OLD);
        mBlurUtilitiesCls = "com.miui.home.launcher.common.BlurUtilities";
        mLauncherFolder2x2IconContainer = "com.miui.home.launcher.folder.LauncherFolder2x2IconContainer";
        initBaseCore();

        // to avoid compilation issues
        if (false) {
            initOS3Hook();
        }
    }

    private void initBaseCore() {
        findAndHookMethod(mBlurUtilitiesCls,
                "isBlurSupported",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        boolean isDefaultIcon = (boolean) XposedHelpers.callStaticMethod(
                            mDeviceConfig,
                                "isDefaultIcon");
                        if (!isDefaultIcon)
                            param.setResult(true);
                    }
                }
        );

        try {
            findAndHookMethod(mLauncherFolder2x2IconContainer,
                    "resolveTopPadding", Rect.class, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            // Rect rect = (Rect) param.args[0];
                            View view = (View) param.thisObject;
                            XposedHelpers.callMethod(view,
                                    "setPadding", 0,
                                    XposedHelpers.callMethod(param.thisObject,
                                            "getMContainerPaddingTop"), 0, 0);
                            param.setResult(null);
                        }
                    }
            );
        } catch (Error | Exception ignore) {
            findAndHookMethod(mDeviceConfig, "isUseDefaultIconFolder1x1", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(true);
                }
            });

            findAndHookMethod(mDeviceConfig, "isUseDefaultIconFolderLarge", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(true);
                }
            });
        }
    }
}
