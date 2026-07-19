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
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui;

import static com.sevtinge.hyperceiler.libhook.base.BaseHook.findAndHookConstructor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class QSColor extends BaseHook {
    private static boolean small = false;
    private static int bgColor = -1;
    private static int color = -1;
    private static boolean big = false;

    private static int bigBgColor = -1;
    private static int bigColor = -1;

    public static void pluginHook(ClassLoader classLoader) {
        String TAG = "QSColor";
        load();
        if (small) {
            try {
                findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, Context.class, Context.class, AttributeSet.class, new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        setObjectReplacement("miui.systemui.plugin", "color", "qs_icon_enabled_color", color);
                    }
                });
            } catch (Exception | Error ignore) {
                findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, Context.class, Context.class, boolean.class, new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        setObjectReplacement("miui.systemui.plugin", "color", "qs_icon_enabled_color", color);
                    }
                });
            }

            // from YunZiA
            com.sevtinge.hyperceiler.libhook.base.BaseHook.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "getActiveBackgroundDrawable", "com.android.systemui.plugins.qs.QSTile$State", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Drawable drawable = (Drawable) param.getResult();
                    if (drawable instanceof GradientDrawable) {
                        ((GradientDrawable) drawable).setColor(bgColor);
                        param.setResult(drawable);
                    }
                }
            });

            // @deprecated 16.0.4.83.0
            try {
                com.sevtinge.hyperceiler.libhook.base.BaseHook.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSTileItemIconView", classLoader, "updateIcon", "com.android.systemui.plugins.qs.QSTile$State", boolean.class, boolean.class, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        com.sevtinge.hyperceiler.libhook.base.BaseHook.setObjectField(param.getThisObject(), "iconColor", color);
                    }
                });
            } catch (Throwable ignore) {}
        }

        if (big) {
            findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSCardItemIconView", classLoader, Context.class, Context.class, AttributeSet.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    setObjectReplacement("miui.systemui.plugin", "color", "qs_icon_enabled_color", bigColor);
                }
            });

            findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader, Context.class, AttributeSet.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    LinearLayout linearLayout = (LinearLayout) param.getThisObject();
                    int cornerRadius = linearLayout.getContext().getResources().getIdentifier("control_center_universal_corner_radius", "dimen", "miui.systemui.plugin");
                    cornerRadiusF = linearLayout.getContext().getResources().getDimensionPixelSize(cornerRadius);
                    id = linearLayout.getContext().getResources().getIdentifier("qs_card_wifi_background_enabled", "drawable", "miui.systemui.plugin");
                }
            });

            com.sevtinge.hyperceiler.libhook.base.BaseHook.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader, "updateBackground", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Object state = com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.getThisObject(), "state");
                    // String spec = (String) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(state, "spec");
                    if (state == null) return; // 系统界面组件会先 null 几次才会获取到值，应该是官方写法有问题
                    int i = com.sevtinge.hyperceiler.libhook.base.BaseHook.getIntField(state, "state");
                    LinearLayout linearLayout = (LinearLayout) param.getThisObject();
                    if (i == 2) {
                        if (id == -1) {
                            XposedLog.w(TAG, "QSCardItemView id is -1!!");
                            return;
                        }
                        Drawable drawable = linearLayout.getContext().getTheme().getResources().getDrawable(id, linearLayout.getContext().getTheme());
                        drawable.setTint(bigBgColor);
                        linearLayout.setBackground(drawable);
                    }
                }
            });

            com.sevtinge.hyperceiler.libhook.base.BaseHook.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader, "setCornerRadius", float.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (cornerRadiusF != -1) {
                        param.getArgs()[0] = cornerRadiusF;
                    }
                }
            });

            com.sevtinge.hyperceiler.libhook.base.BaseHook.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader, "updateState", "com.android.systemui.plugins.qs.QSTile$State", boolean.class, boolean.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Object state = param.getArgs()[0];
                    int i = com.sevtinge.hyperceiler.libhook.base.BaseHook.getIntField(state, "state");
                    if (i == 2) {
                        Context context = ((LinearLayout) param.getThisObject()).getContext();
                        TextView textView, textView1;

                        try {
                            int title = context.getResources().getIdentifier("title", "id", "miui.systemui.plugin");
                            int status = context.getResources().getIdentifier("status", "id", "miui.systemui.plugin");

                            textView = (TextView) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(param.getThisObject(), "_$_findCachedViewById", title);
                            textView1 = (TextView) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(param.getThisObject(), "_$_findCachedViewById", status);
                        } catch (Throwable ignore) {
                            // 17.0.1.19.4 变更
                            Object binding = com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(param.getThisObject(), "getBinding");

                            textView = (TextView) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(binding, "title");
                            textView1 = (TextView) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(binding, "status");
                        }
                        textView.setTextColor(bigColor);
                        textView1.setTextColor(bigColor);
                    }
                }
            });

            // @deprecated unknown version
            try {
                com.sevtinge.hyperceiler.libhook.base.BaseHook.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemIconView", classLoader, "setIcon", "com.android.systemui.plugins.qs.QSTile$State", boolean.class, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        com.sevtinge.hyperceiler.libhook.base.BaseHook.setObjectField(param.getThisObject(), "iconColor", bigColor);
                    }
                });
            } catch (Throwable ignore) {}
        }
    }

    public static int id = -1;
    public static float cornerRadiusF = -1;

    public static void load() {
        small = PrefsBridge.getBoolean("system_ui_control_center_qs_open_color");
        big = PrefsBridge.getBoolean("system_ui_control_center_qs_big_open_color");
        bgColor = PrefsBridge.getInt("system_ui_control_center_qs_bg_color", -1);
        color = PrefsBridge.getInt("system_ui_control_center_qs_color", -1);
        bigBgColor = PrefsBridge.getInt("system_ui_control_center_qs_big_bg_color", -1);
        bigColor = PrefsBridge.getInt("system_ui_control_center_qs_big_color", -1);
    }

    @Override
    public void init() {
        load();
        if (small) {
            findAndHookConstructor("com.android.systemui.qs.tileimpl.MiuiQSIconViewImpl", Context.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.setObjectField(param.getThisObject(), "mIconColorEnabled", color);
                    setObjectReplacement("com.android.systemui", "color", "qs_tile_icon_enabled_color", color);
                }
            });
        }
    }
}
