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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class QSColor extends BaseHook {
    private static boolean small = false;
    private static int bgColor = -1;
    private static int color = -1;
    private static boolean big = false;

    private static int bigBgColor = -1;
    private static int bigColor = -1;

    @Override
    public void init() throws NoSuchMethodException {
        load();
        if (small) {
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
    }

    public static int id = -1;
    public static float cornerRadiusF = -1;

    public static void load() {
        small = mPrefsMap.getBoolean("system_ui_control_center_qs_open_color");
        big = mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color");
        bgColor = mPrefsMap.getInt("system_ui_control_center_qs_bg_color", -1);
        color = mPrefsMap.getInt("system_ui_control_center_qs_color", -1);
        bigBgColor = mPrefsMap.getInt("system_ui_control_center_qs_big_bg_color", -1);
        bigColor = mPrefsMap.getInt("system_ui_control_center_qs_big_color", -1);
    }

    public static void pluginHook(ClassLoader classLoader) {
        String TAG = "QSColor";
        load();
        if (small) {
            XposedHelpers.findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader,
                    Context.class, Context.class, AttributeSet.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            XposedInit.mResHook.setObjectReplacement("miui.systemui.plugin", "color", "qs_icon_enabled_color", color);
                        }
                    }
            );

            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView",
                    classLoader, "updateIcon",
                    "com.android.systemui.plugins.qs.QSTile$State", boolean.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            XposedHelpers.setObjectField(param.thisObject, "iconColor", color);
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

        if (big) {
            XposedHelpers.findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSCardItemIconView", classLoader,
                    Context.class, Context.class, AttributeSet.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            XposedInit.mResHook.setObjectReplacement("miui.systemui.plugin", "color", "qs_icon_enabled_color", bigColor);
                        }
                    }
            );

            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemIconView", classLoader,
                    "setIcon", "com.android.systemui.plugins.qs.QSTile$State", boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            XposedHelpers.setObjectField(param.thisObject, "iconColor", bigColor);
                        }
                    }
            );

            XposedHelpers.findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader,
                    Context.class, AttributeSet.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            LinearLayout linearLayout = (LinearLayout) param.thisObject;
                            int cornerRadius = linearLayout.getContext().getResources().getIdentifier(
                                    "control_center_universal_corner_radius", "dimen", "miui.systemui.plugin");
                            cornerRadiusF = linearLayout.getContext().getResources().getDimensionPixelSize(cornerRadius);
                            id = linearLayout.getContext().getResources().getIdentifier("qs_card_wifi_background_enabled",
                                    "drawable", "miui.systemui.plugin");
                        }
                    }
            );

            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader,
                    "updateBackground",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object state = XposedHelpers.getObjectField(param.thisObject, "state");
                            String spec = (String) XposedHelpers.getObjectField(state, "spec");
                            int i = XposedHelpers.getIntField(state, "state");
                            LinearLayout linearLayout = (LinearLayout) param.thisObject;
                            if (i == 2) {
                                if (id == -1) {
                                    logE(TAG, "id is -1!!");
                                    return;
                                }
                                Drawable drawable = linearLayout.getContext().getTheme().getResources().getDrawable(id, linearLayout.getContext().getTheme());
                                drawable.setTint(bigBgColor);
                                linearLayout.setBackground(drawable);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader,
                    "setCornerRadius", float.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (cornerRadiusF != -1) {
                                param.args[0] = cornerRadiusF;
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader, "updateState",
                    "com.android.systemui.plugins.qs.QSTile$State", boolean.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object state = param.args[0];
                            int i = XposedHelpers.getIntField(state, "state");
                            if (i == 2) {
                                Context context = ((LinearLayout) param.thisObject).getContext();
                                int title = context.getResources().getIdentifier("title", "id", "miui.systemui.plugin");
                                int status = context.getResources().getIdentifier("status", "id", "miui.systemui.plugin");
                                TextView textView = (TextView) XposedHelpers.callMethod(param.thisObject, "_$_findCachedViewById", title);
                                TextView textView1 = (TextView) XposedHelpers.callMethod(param.thisObject, "_$_findCachedViewById", status);
                                textView.setTextColor(bigColor);
                                textView1.setTextColor(bigColor);
                            }
                        }
                    }
            );
        }
    }
}
