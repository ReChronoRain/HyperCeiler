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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.BaseXposedInit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class QSColor extends BaseHook {
    private static int bgColor = -1;
    private static int color = -1;

    @Override
    public void init() throws NoSuchMethodException {
        bgColor = mPrefsMap.getInt("system_ui_control_center_qs_bg_color", -1);
        color = mPrefsMap.getInt("system_ui_control_center_qs_color", -1);
        findAndHookConstructor("com.android.systemui.qs.tileimpl.MiuiQSIconViewImpl",
                Context.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        XposedHelpers.setObjectField(param.thisObject, "mIconColorEnabled", color);
                        mResHook.setObjectReplacement("com.android.systemui", "color", "qs_tile_icon_enabled_color", color);
                    }
                }
        );
    }

    public static void pluginHook(ClassLoader classLoader) {
        bgColor = mPrefsMap.getInt("system_ui_control_center_qs_bg_color", -1);
        color = mPrefsMap.getInt("system_ui_control_center_qs_color", -1);
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView",
                classLoader, "updateIcon",
                "com.android.systemui.plugins.qs.QSTile$State", boolean.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedHelpers.setObjectField(param.thisObject, "iconColor", color);
                        BaseXposedInit.mResHook.setObjectReplacement("miui.systemui.plugin", "color", "qs_icon_enabled_color", color);
                    }
                }
        );
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader,
                "getActiveBackgroundDrawable", "com.android.systemui.plugins.qs.QSTile$State",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        GradientDrawable drawable = (GradientDrawable) param.getResult();
                        drawable.setColor(bgColor);
                        param.setResult(drawable);
                    }
                }
        );
    }
}
