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

import static com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.getModuleRes;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.hyperceiler.hook.R;
import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.util.ArrayList;
import java.util.Iterator;

import de.robv.android.xposed.XposedHelpers;

public class CCGrid extends BaseHook {
    private static final int cols = mPrefsMap.getInt("system_control_center_cc_columns", 4); // 列数
    private static final int rows = mPrefsMap.getInt("system_control_center_cc_rows", 4); // 行数
    private static final boolean label = mPrefsMap.getBoolean("system_control_center_qs_tile_label"); // 移除标题
    private static float scaledTileWidthDim = -1f;

    @Override
    public void init() throws NoSuchMethodException {
        final String pkg = lpparam.packageName;
        if (cols > 4) {
            mResHook.setObjectReplacement(pkg, "dimen", "qs_control_tiles_columns", cols);
        }

        findAndHookMethod("com.android.systemui.SystemUIApplication",
                "onCreate",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Context mContext = (Context) XposedHelpers.callMethod(param.thisObject,
                                "getApplicationContext");
                        Resources resources = mContext.getResources();
                        float density = resources.getDisplayMetrics().density;
                        int tileWidthResId = resources.getIdentifier("qs_control_center_tile_width", "dimen", "com.android.systemui");
                        float tileWidthDim = resources.getDimension(tileWidthResId);
                        if (cols > 4) {
                            tileWidthDim /= density;
                            scaledTileWidthDim = tileWidthDim * 4 / cols;
                            mResHook.setDensityReplacement(pkg, "dimen", "qs_control_center_tile_width",
                                    scaledTileWidthDim);
                            mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_control_center_tile_width",
                                    scaledTileWidthDim);
                            mResHook.setDensityReplacement(pkg, "dimen", "qs_control_tile_icon_bg_size",
                                    scaledTileWidthDim);
                            mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_control_tile_icon_bg_size",
                                    scaledTileWidthDim);
                            mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85f);
                        }
                    }

                }
        );
    }

    public static void loadCCGrid(ClassLoader pluginLoader) {
        if (cols > 4) {
            findAndHookConstructor("miui.systemui.controlcenter.qs.QSPager",
                    pluginLoader, Context.class, AttributeSet.class,
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            XposedHelpers.setObjectField(param.thisObject, "columns", cols);
                        }
                    }
            );
        }
        if (!label) {
            findAndHookMethod("miui.systemui.controlcenter.qs.tileview.StandardTileView", pluginLoader,
                    "createLabel", boolean.class,
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            Object label = XposedHelpers.getObjectField(param.thisObject, "label");
                            if (label != null) {
                                TextView lb = (TextView) label;
                                lb.setMaxLines(1);
                                lb.setSingleLine(true);
                                lb.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                                lb.setMarqueeRepeatLimit(0);
                                View labelContainer = (View) XposedHelpers.getObjectField(param.thisObject, "labelContainer");
                                labelContainer.setPadding(4, 0, 4, 0);

                            }
                        }
                    }
            );
        }
        if (rows != 4) {
            findAndHookMethod("miui.systemui.controlcenter.qs.QSPager", pluginLoader,
                    "distributeTiles",
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            boolean collapse = XposedHelpers.getBooleanField(param.thisObject, "collapse");
                            if (collapse) {
                                ArrayList<?> pages = (ArrayList<?>) XposedHelpers.getObjectField(param.thisObject, "pages");
                                for (Object tileLayoutImpl : pages) {
                                    XposedHelpers.callMethod(tileLayoutImpl, "removeTiles");
                                }
                                ArrayList<Object> pageTiles = new ArrayList<>();
                                int currentRow = 2;
                                ArrayList<?> records = (ArrayList<?>) XposedHelpers.getObjectField(param.thisObject, "records");
                                Iterator<?> it2 = records.iterator();
                                int i3 = 0;
                                int pageNow = 0;
                                Object bigHeader = XposedHelpers.getObjectField(param.thisObject, "header");
                                while (it2.hasNext()) {
                                    pageTiles.add(it2.next());
                                    i3++;
                                    if (i3 >= cols) {
                                        currentRow++;
                                        i3 = 0;
                                    }
                                    if (currentRow >= rows || !it2.hasNext()) {
                                        XposedHelpers.callMethod(pages.get(pageNow), "setTiles", pageTiles,
                                                pageNow == 0 ? bigHeader : null);
                                        pageTiles.clear();
                                        int totalRows = XposedHelpers.getIntField(param.thisObject, "rows");
                                        if (currentRow > totalRows) {
                                            XposedHelpers.setObjectField(param.thisObject, "rows", currentRow);
                                        }
                                        if (it2.hasNext()) {
                                            pageNow++;
                                            currentRow = 0;
                                        }
                                    }
                                }
                                Iterator<?> it3 = pages.iterator();
                                while (it3.hasNext()) {
                                    boolean isEmpty = (boolean) XposedHelpers.callMethod(it3.next(), "isEmpty");
                                    if (isEmpty) {
                                        it3.remove();
                                    }
                                }
                                Object pageIndicator = XposedHelpers.getObjectField(param.thisObject, "pageIndicator");
                                if (pageIndicator != null) {
                                    XposedHelpers.callMethod(pageIndicator, "setNumPages", pages.size());
                                }
                                Object adapter = XposedHelpers.getObjectField(param.thisObject, "adapter");
                                XposedHelpers.callMethod(param.thisObject, "setAdapter", adapter);
                            }
                        }
                    }
            );
        }
        // 移除磁贴标题相关
        if (mPrefsMap.getBoolean("system_control_center_qs_tile_label")) {
            mHideCCLabels(pluginLoader);
        }

        // 新控制中心矩形圆角
        if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect")) {
            mResHook.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_restricted",
                    R.drawable.ic_qs_tile_bg_temporary_closure);
            mResHook.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_unavailable",
                    R.drawable.ic_qs_tile_bg_disabled);
            mResHook.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_disabled",
                    R.drawable.ic_qs_tile_bg_disabled);
            mResHook.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_warning",
                    R.drawable.ic_qs_tile_bg_warning);
            mCCTileCornerHook(pluginLoader);
        }
    }

    private static void mHideCCLabels(ClassLoader pluginLoader) {
        mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85f);
        Class<?> mQSController = XposedHelpers.findClassIfExists(
                "miui.systemui.controlcenter.qs.tileview.StandardTileView",
                pluginLoader
        );
        hookAllMethods(mQSController, "init",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (param.args.length != 1) return;
                        View mLabelContainer = (View) XposedHelpers.getObjectField(param.thisObject, "labelContainer");
                        mLabelContainer.setVisibility(View.GONE);
                    }
                }
        );
    }

    private static void mCCTileCornerHook(ClassLoader pluginLoader) {
        findAndHookMethod("miui.systemui.controlcenter.qs.tileview.ExpandableIconView", pluginLoader,
                "setCornerRadius", float.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getPluginContext");
                        var radius = 18f;
                        if (scaledTileWidthDim > 0) {
                            radius *= scaledTileWidthDim / 65;
                        }
                        param.args[0] = mContext.getResources().getDisplayMetrics().density * radius;
                    }
                }
        );
        findAndHookMethod("miui.systemui.dagger.PluginComponentFactory", pluginLoader,
                "create", Context.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Context mContext = (Context) param.args[0];
                        Resources res = mContext.getResources();
                        int enabledTileBackgroundResId = res.getIdentifier("qs_background_enabled", "drawable", "miui.systemui.plugin");
                        int enabledTileColorResId = res.getIdentifier("qs_enabled_color", "color", "miui.systemui.plugin");
                        int tintColor = res.getColor(enabledTileColorResId, null);
                        MethodHook imgHook = new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                int resInt = (int) param.args[0];
                                if (resInt == enabledTileBackgroundResId && resInt != 0) {
                                    try {
                                        Drawable enableTile = getModuleRes(mContext)
                                                .getDrawable(R.drawable.ic_qs_tile_bg_enabled, null);
                                        enableTile.setTint(tintColor);
                                        param.setResult(enableTile);
                                    } catch (PackageManager.NameNotFoundException e) {
                                        logE("CCGrid", e);
                                    }
                                }
                            }
                        };

                        findAndHookMethod("android.content.res.Resources", pluginLoader,
                                "getDrawable", int.class, imgHook);
                        findAndHookMethod("android.content.res.Resources.Theme", pluginLoader,
                                "getDrawable", int.class, imgHook);
                    }
                }
        );
    }
}
