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
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 磁贴高级材质
 *
 * @author 焕晨HChen
 */
public class QsTileSuperBlur extends HCBase {
    private static int[] listItemsBlendColors = null;
    private static int[] seekbarFgBlendColors = null;
    private static int[] ringerIconBlendColors = null;
    private static int[] seekbarAndRingerBgBlendColors = null;
    private static Class<?> modeClass;
    private static Class<?> miBlurCompatClass;
    private static Object NORMAL;
    private static Object SORT;
    private static Object EDIT;
    private static final ConcurrentHashMap<View, Boolean> standardBtn2StateMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<View, Boolean> icon2StateMap = new ConcurrentHashMap<>();
    private static boolean isOS1 = false;
    private static final String os2 = "MainPanelController$Mode";
    private static final String os1 = "MainPanelModeController$MainPanelMode";

    @Override
    protected void init() {
    }

    public static void initQsTileSuperBlur(@NonNull ClassLoader classLoader) {
        isOS1 = !existsClass("miui.systemui.controlcenter.panel.main.MainPanelController$Mode", classLoader);
        modeClass = findClass("miui.systemui.controlcenter.panel.main." + (isOS1 ? os1 : os2), classLoader);

        miBlurCompatClass = findClass("miui.systemui.util.MiBlurCompat", classLoader);
        NORMAL = getStaticField(modeClass, (isOS1 ? "MODE_" : "") + "NORMAL");
        SORT = getStaticField(modeClass, (isOS1 ? "MODE_" : "") + "SORT");
        EDIT = getStaticField(modeClass, (isOS1 ? "MODE_" : "") + "EDIT");

        hookConstructor("miui.systemui.controlcenter.qs.tileview.QSTileItemView", classLoader,
            Context.class, AttributeSet.class, int.class, int.class,
            new IHook() {
                @Override
                public void after() {
                    initRes((Context) getArg(0));

                    Object state = getThisField("state");
                    if (state != null) {
                        updateBlendColors(
                            (View) callThisMethod("getBlendTarget"),
                            state,
                            false
                        );
                    }
                }
            }
        );

        hookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemView", classLoader,
            "onModeChanged",
            "miui.systemui.controlcenter.panel.main." + (isOS1 ? os1 : os2), boolean.class,
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
                                (View) callThisMethod("getBlendTarget"),
                                state,
                                false
                            );
                    }
                    callThisMethod("onStateUpdated", false);
                    callThisMethod("changeExpand");
                    callThisMethod("showLabel", callThisMethod("getShowLabel"), getArg(1));
                    callThisMethod("updateMark", getArg(1));
                    returnNull();
                }
            }
        );

        hookMethod("miui.systemui.controlcenter.panel.main.qs.QSItemViewHolder", classLoader,
            "updateBlendBlur",
            new IHook() {
                @Override
                public void before() {
                    Object qsItemView = callThisMethod("getQsItemView");
                    View blendTarget = (View) callMethod(qsItemView, "getBlendTarget");
                    Object state = getField(qsItemView, "state");
                    Object mode = getField(qsItemView, "mode");
                    if (state != null) {
                        updateBlendColors(
                            blendTarget,
                            state,
                            mode == EDIT
                        );
                    } else return;
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
                            (View) callThisMethod("getBlendTarget"),
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

        hookMethod("miui.systemui.controlcenter.panel.main.qs.QSCardViewHolder", classLoader,
            "updateBlendBlur",
            new IHook() {
                @Override
                public void before() {
                    Object qsItemView = callThisMethod("getQsItemView");
                    View blendTarget = (View) callMethod(qsItemView, "getBlendTarget");
                    Object state = getField(qsItemView, "state");
                    if (state != null) {
                        updateBlendColors(
                            blendTarget,
                            state,
                            false
                        );
                    } else return;
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

        hookMethod("com.android.systemui.miui.volume.MiuiRingerModeLayout$RingerButtonHelper", classLoader,
            "updateState",
            new IHook() {
                @Override
                public void before() {
                    ImageView mIcon = (ImageView) getThisField("mIcon");
                    View mStandardView = (View) getThisField("mStandardView");
                    boolean state = (boolean) getThisField("mState");
                    // boolean lastState = (boolean) getThisField("mLastState");

                    standardBtn2StateMap.put(mStandardView, state);
                    icon2StateMap.put(mIcon, state);
                }
            }
        );

        hookMethod("com.android.systemui.miui.volume.Util", classLoader,
            "setMiViewBlurAndBlendColor",
            View.class, boolean.class, Context.class, int.class, int[].class, boolean.class,
            new IHook() {
                @Override
                public void before() {
                    if (getArg(0) instanceof LinearLayout view) {
                        String name = view.getResources().getResourceEntryName(view.getId());
                        if (Objects.equals(name, "miui_standard_btn")) {
                            Boolean state = standardBtn2StateMap.get(view);
                            if (state == null) return;
                            if (state) {
                                initRes(view.getContext());
                                setArg(4, ringerIconBlendColors);
                            }
                        }
                    }
                }
            }
        );

        hookMethod("com.android.systemui.miui.volume.Util", classLoader,
            "setMiViewBlurAndBlendColor",
            View.class, int.class, int[].class,
            new IHook() {
                @Override
                public void before() {
                    if (getArg(0) instanceof ImageView imageView && (int) getArg(1) == 0) {
                        String name = imageView.getResources().getResourceEntryName(imageView.getId());
                        if (Objects.equals(name, "icon")) {
                            Boolean state = icon2StateMap.get(imageView);
                            if (state == null) return;
                            if (state) {
                                initRes(imageView.getContext());
                                setArg(1, 3);
                                setArg(2, seekbarAndRingerBgBlendColors);
                            }
                        }
                    }
                }
            }
        );
    }

    private static void initRes(Context context) {
        int listItemsBlendColorsId = -1, seekbarFgBlendColorsId = -1, ringerIconBlendColorsId = -1, seekbarAndRingerBgBlendColorsId = -1;
        if (listItemsBlendColors == null || seekbarFgBlendColors == null || ringerIconBlendColors == null || seekbarAndRingerBgBlendColors == null) {
            listItemsBlendColorsId = context.getResources().getIdentifier("control_center_list_items_blend_colors", "array", context.getPackageName());
            seekbarFgBlendColorsId = context.getResources().getIdentifier("miui_seekbar_fg_blend_colors_collapsed", "array", context.getPackageName());
            ringerIconBlendColorsId = context.getResources().getIdentifier("miui_ringer_icon_blend_colors_collapsed", "array", context.getPackageName());
            seekbarAndRingerBgBlendColorsId = context.getResources().getIdentifier("miui_seekbar_and_ringer_bg_blend_colors_collapsed", "array", context.getPackageName());
            listItemsBlendColors = context.getResources().getIntArray(listItemsBlendColorsId);
            seekbarFgBlendColors = context.getResources().getIntArray(seekbarFgBlendColorsId);
            ringerIconBlendColors = context.getResources().getIntArray(ringerIconBlendColorsId);
            seekbarAndRingerBgBlendColors = context.getResources().getIntArray(seekbarAndRingerBgBlendColorsId);
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
        // callStaticMethod(miBlurCompatClass, "setMiViewBlurModeCompat", view, 1);
        callStaticMethod(miBlurCompatClass, "setMiBackgroundBlendColors", view, blendColors);
    }
}
