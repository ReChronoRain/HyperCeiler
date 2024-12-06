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

import static com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.tool.HookTool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class CCGridForHyperOS {
    private static final float radius = (float) mPrefsMap.getInt("system_ui_control_center_rounded_rect_radius", 72);

    private static Drawable warningD = null;
    private static Drawable enabledD = null;
    private static Drawable restrictedD = null;
    private static Drawable disabledD = null;
    private static Drawable unavailableD = null;
    private static int configuration = 0;
    private static int orientation;

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
                        XposedHelpers.callMethod(param.thisObject, "updateResources");
                    }
                }
        );

        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader,
                "updateResources",
                new HookTool.MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        orientation = ((View) param.thisObject).getContext().getResources().getConfiguration().orientation;

                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        Set<String> targetMethods = new HashSet<>(Arrays.asList(
                                "miui.systemui.controlcenter.qs.tileview.QSTileItemView.updateState",
                                "miui.systemui.controlcenter.panel.main.recyclerview.MainPanelItemViewHolder.onSuperSaveModeChanged",
                                "miui.systemui.controlcenter.qs.tileview.QSTileItemView.updateCustomizeState",
                                "miui.systemui.controlcenter.qs.tileview.QSTileItemView.onModeChanged"
                        ));

                        boolean isMethodFound = Arrays.stream(stackTrace)
                                .anyMatch(element -> targetMethods.contains(element.getClassName() + "." + element.getMethodName()));

                        if (configuration == orientation && isMethodFound) return;

                        Context pluginContext = (Context) XposedHelpers.getObjectField(param.thisObject, "pluginContext");
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

                        configuration = orientation;
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
}