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
package com.sevtinge.hyperceiler.libhook.rules.home.folder;

import android.graphics.Rect;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class UnlockBlurSupported extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.common.BlurUtilities",
            "isBlurSupported",
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    boolean isDefaultIcon = (boolean) callStaticMethod(
                        findClassIfExists("com.miui.home.launcher.DeviceConfig"),
                        "isDefaultIcon");
                    if (!isDefaultIcon)
                        param.setResult(true);
                }
            }
        );

        try {
            findAndHookMethod("com.miui.home.launcher.folder.LauncherFolder2x2IconContainer",
                "resolveTopPadding", Rect.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        // Rect rect = (Rect) param.args[0];
                        View view = (View) param.getThisObject();
                        callMethod(view,
                            "setPadding", 0,
                            callMethod(param.getThisObject(),
                                "getMContainerPaddingTop"), 0, 0);
                        param.setResult(null);
                    }
                }
            );
        } catch (Error | Exception ignore) {
            findAndHookMethod("com.miui.home.launcher.DeviceConfig", "isUseDefaultIconFolder1x1", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(true);
                }
            });

            findAndHookMethod("com.miui.home.launcher.DeviceConfig", "isUseDefaultIconFolderLarge", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(true);
                }
            });
        }
    }
}
