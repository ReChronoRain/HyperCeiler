/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class HideNavigationBar extends BaseHook {
    @Override
    public void init() {
        // Keep the navigation window and its input region, including rotated handles.
        hookAllConstructors("com.android.systemui.navigationbar.gestural.NavigationHandle", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                ((View) param.getThisObject()).setWillNotDraw(true);
            }
        });

        if (isMoreHyperOSVersion(3f)) {
            hideNavigationBarInsets();
        }

        String decorationPackage = "com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.";
        Class<?> bottomDecoration = findClassIfExists(decorationPackage + "MiuiDecorationBottom");
        if (bottomDecoration == null) {
            return;
        }

        // The native caption policy handles fullscreen/split/home; freeform has its own branch.
        findAndHookMethod(bottomDecoration, "isHideNavigationHandle", returnConstant(true));

        // SystemUI is AOT-compiled, so callers may have inlined this small predicate.
        deoptimizeMethods(bottomDecoration, "needCaption");
        Class<?> homeDecoration = findClassIfExists(decorationPackage + "MiuiDecorationHomeBottom");
        if (homeDecoration != null) {
            deoptimizeMethods(homeDecoration, "shouldSkipByDeviceConditions", "needCaption");
        }
    }

    private void hideNavigationBarInsets() {
        Class<?> navigationBar = findClassIfExists("com.android.systemui.navigationbar.views.NavigationBar");
        if (navigationBar == null) {
            return;
        }

        findAndHookMethod(navigationBar, "getBarLayoutParamsForRotation", int.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                try {
                    Object result = param.getResult();
                    if (!(result instanceof WindowManager.LayoutParams layoutParams)) {
                        return;
                    }

                    Object value = getObjectField(layoutParams, "providedInsets");
                    if (!(value instanceof Object[] providers)) {
                        return;
                    }

                    int navigationBars = WindowInsets.Type.navigationBars();
                    int tappableElement = WindowInsets.Type.tappableElement();
                    for (Object provider : providers) {
                        if (provider == null) {
                            continue;
                        }
                        int type = (int) callMethod(provider, "getType");
                        if (type == navigationBars || type == tappableElement) {
                            callMethod(provider, "setInsetsSize", Insets.NONE);
                        }
                    }
                } catch (Throwable t) {
                    debugCallbackError("hide navigation bar insets", t);
                }
            }
        });
    }
}
