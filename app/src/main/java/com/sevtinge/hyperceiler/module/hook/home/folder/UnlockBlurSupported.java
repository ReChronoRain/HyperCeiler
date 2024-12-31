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
package com.sevtinge.hyperceiler.module.hook.home.folder;

import android.graphics.Rect;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class UnlockBlurSupported extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.common.BlurUtilities",
                "isBlurSupported",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        boolean isDefaultIcon = (boolean) XposedHelpers.callStaticMethod(
                                findClassIfExists("com.miui.home.launcher.DeviceConfig"),
                                "isDefaultIcon");
                        if (!isDefaultIcon)
                            param.setResult(true);
                    }
                }
        );

        try {
            findAndHookMethod("com.miui.home.launcher.folder.LauncherFolder2x2IconContainer",
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
            findAndHookMethod("com.miui.home.launcher.DeviceConfig", "isUseDefaultIconFolder1x1", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });

            findAndHookMethod("com.miui.home.launcher.DeviceConfig", "isUseDefaultIconFolderLarge", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
    }
}
