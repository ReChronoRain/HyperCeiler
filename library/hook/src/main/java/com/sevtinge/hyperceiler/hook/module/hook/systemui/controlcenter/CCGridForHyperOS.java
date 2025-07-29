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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logD;
import static com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class CCGridForHyperOS {
    private static final float radius = (float) mPrefsMap.getInt("system_ui_control_center_rounded_rect_radius", 72);

    private static Drawable warningD = null;
    private static Drawable enabledD = null;
    private static Drawable restrictedD = null;
    private static Drawable disabledD = null;
    private static Drawable unavailableD = null;

    public static void initCCGridForHyperOS(ClassLoader classLoader) {
        Class<?> clazz = XposedHelpers.findClass("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader);
        XposedHelpers.findAndHookMethod(clazz, "getCornerRadius",
                new HookTool.MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        param.setResult((float) mPrefsMap.getInt("system_ui_control_center_rounded_rect_radius", 72));
                    }
                }
        );
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "updateIcon",
                "com.android.systemui.plugins.qs.QSTile$State", boolean.class, boolean.class,
                new HookTool.MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if ((boolean) param.args[1] && (boolean) param.args[2] || warningD == null || enabledD == null || restrictedD == null || disabledD == null || unavailableD == null) {
                            getPluginResources((Context) XposedHelpers.getObjectField(param.thisObject, "pluginContext"));
                            // XposedHelpers.callMethod(param.thisObject, "updateResources");       // 为什么能跑起来？
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader,
                "updateResources",
                new HookTool.MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (warningD == null || enabledD == null || restrictedD == null || disabledD == null || unavailableD == null) getPluginResources((Context) XposedHelpers.getObjectField(param.thisObject, "pluginContext"));
                        if (warningD != null) {
                            GradientDrawable warningG = (GradientDrawable) warningD;
                            warningG.setCornerRadius(radius);
                        }
                        if (enabledD != null) {
                            GradientDrawable enabledG = (GradientDrawable) enabledD;
                            enabledG.setCornerRadius(radius);
                        }
                        if (restrictedD != null) {
                            GradientDrawable restrictedG = (GradientDrawable) restrictedD;
                            restrictedG.setCornerRadius(radius);
                        }
                        if (disabledD != null) {
                            GradientDrawable disabledG = (GradientDrawable) disabledD;
                            disabledG.setCornerRadius(radius);
                        }
                        if (unavailableD != null) {
                            GradientDrawable unavailableG = (GradientDrawable) unavailableD;
                            unavailableG.setCornerRadius(radius);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader,
                "setCornerRadius", float.class,
                new HookTool.MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
        );
    }

    private static void getPluginResources(Context pluginContext) {
        int warning = pluginContext.getResources().getIdentifier("qs_background_warning", "drawable", "miui.systemui.plugin");
        int enabled = pluginContext.getResources().getIdentifier("qs_background_enabled", "drawable", "miui.systemui.plugin");
        int restricted = pluginContext.getResources().getIdentifier("qs_background_restricted", "drawable", "miui.systemui.plugin");
        int disabled = pluginContext.getResources().getIdentifier("qs_background_disabled", "drawable", "miui.systemui.plugin");
        int unavailable = pluginContext.getResources().getIdentifier("qs_background_unavailable", "drawable", "miui.systemui.plugin");
        warningD = pluginContext.getTheme().getDrawable(warning);
        enabledD = pluginContext.getTheme().getDrawable(enabled);
        restrictedD = pluginContext.getTheme().getDrawable(restricted);
        disabledD = pluginContext.getTheme().getDrawable(disabled);
        unavailableD = pluginContext.getTheme().getDrawable(unavailable);
    }
}
