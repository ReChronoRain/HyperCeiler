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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;

/**
 * 磁贴高级材质
 */
public class QsTileSuperBlur extends HCBase {
    private static int listItemsBlendColorsId = -1;
    private static int seekbarFgBlendColorsId = -1;
    private static int[] listItemsBlendColors = null;
    private static int[] seekbarFgBlendColors = null;
    private static Class<?> modeClass;
    private static Class<?> miBlurCompatClass;
    private static Object NORMAL;
    private static Object SORT;
    private static Object EDIT;

    @Override
    protected void init() {
    }

    public static void initQsTileSuperBlur(@NonNull ClassLoader classLoader) {
        modeClass = findClass("miui.systemui.controlcenter.panel.main.MainPanelController$Mode", classLoader);
        miBlurCompatClass = findClass("miui.systemui.util.MiBlurCompat", classLoader);
        NORMAL = getStaticField(modeClass, "NORMAL");
        SORT = getStaticField(modeClass, "SORT");
        EDIT = getStaticField(modeClass, "EDIT");

        hookConstructor("miui.systemui.controlcenter.qs.tileview.QSTileItemView", classLoader,
            Context.class, AttributeSet.class, int.class, int.class,
            new IHook() {
                @Override
                public void after() {
                    initRes((Context) getArg(0));

                    Object state = getThisField("state");
                    if (state != null) {
                        updateBlendColors(
                            (View) callMethod(
                                callThisMethod("getIcon"),
                                "getIcon"
                            ),
                            state,
                            false
                        );
                    }
                }
            }
        );

        hookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemView", classLoader,
            "onModeChanged",
            "miui.systemui.controlcenter.panel.main.MainPanelController$Mode", boolean.class,
            new IHook() {
                @Override
                public void before() {
                    Object mode = getArg(0);
                    if (mode == null) return;

                    setThisField("mode", mode);
                    if (mode == EDIT) {
                        updateBlendColors(
                            (View) callMethod(
                                callThisMethod("getIcon"),
                                "getIcon"
                            ),
                            null,
                            true
                        );
                    } else {
                        Object state = getThisField("state");
                        if (state != null)
                            updateBlendColors(
                                (View) callMethod(
                                    callThisMethod("getIcon"),
                                    "getIcon"
                                ),
                                state,
                                false
                            );
                        callThisMethod("onStateUpdated", false);
                    }
                    callThisMethod("changeExpand");
                    callThisMethod("showLabel", callThisMethod("getShowLabel"), getArg(1));
                    callThisMethod("updateMark", getArg(1));
                    returnNull();
                }
            }
        );

        hookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemView", classLoader,
            "updateState",
            "com.android.systemui.plugins.qs.QSTile$State", boolean.class, boolean.class,
            new IHook() {
                @Override
                public void before() {
                    setThisField("state", getArg(0));
                    Object state = getArg(0);
                    Object mode = getThisField("mode");

                    if (state != null && callThisMethod("getIcon") != null) {
                        updateBlendColors(
                            (View) callMethod(
                                callThisMethod("getIcon"),
                                "getIcon"
                            ),
                            state,
                            mode == EDIT
                        );
                    }
                    if (mode != NORMAL) {
                        returnNull();
                        return;
                    }
                    callThisMethod("onStateUpdated", getArg(2));
                    returnNull();
                }
            }
        );

        hookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader,
            "updateBackground",
            new IHook() {
                @Override
                public void after() {
                    initRes((Context) callThisMethod("getContext"));
                    Object state = getThisField("state");
                    if (state == null) return;

                    callStaticMethod(
                        miBlurCompatClass,
                        "setMiBackgroundBlendColors",
                        thisObject(),
                        ((int) getField(state, "state")) == 2 ?
                            seekbarFgBlendColors :
                            listItemsBlendColors
                    );
                }
            }
        );
    }

    private static void initRes(Context context) {
        if (listItemsBlendColorsId == -1 || seekbarFgBlendColorsId == -1) {
            listItemsBlendColorsId = context.getResources().getIdentifier("control_center_list_items_blend_colors", "array", context.getPackageName());
            seekbarFgBlendColorsId = context.getResources().getIdentifier("miui_seekbar_fg_blend_colors_collapsed", "array", context.getPackageName());
            listItemsBlendColors = context.getResources().getIntArray(listItemsBlendColorsId);
            seekbarFgBlendColors = context.getResources().getIntArray(seekbarFgBlendColorsId);
        }
    }

    private static void updateBlendColors(View view, Object state, boolean isEditMode) {
        if (isEditMode) {
            setMiBackgroundBlendColors(view, listItemsBlendColors);
        } else {
            setMiBackgroundBlendColors(
                view,
                ((int) getField(state, "state")) == 2 ?
                    seekbarFgBlendColors :
                    listItemsBlendColors
            );
        }
    }

    private static void setMiBackgroundBlendColors(View view, int[] blendColors) {
        callStaticMethod(miBlurCompatClass, "setMiBackgroundBlendColors", view, blendColors);
    }
}
