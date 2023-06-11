package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SdkHelper.isAndroidMoreVersion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.ArrayList;
import java.util.Iterator;

import de.robv.android.xposed.XposedHelpers;

public class CCGrid extends BaseHook {
    private static float scaledTileWidthDim = -1f;
    private static ClassLoader pluginLoader = null;

    @Override
    @SuppressLint("DiscouragedApi")
    public void init() {
        int cols = mPrefsMap.getInt("system_control_center_cc_columns", 4);
        int rows = mPrefsMap.getInt("system_control_center_cc_rows", 4);
        if (cols > 4) {
            mResHook.setObjectReplacement(lpparam.packageName, "dimen", "qs_control_tiles_columns", cols);
        }

        Helpers.findAndHookMethod("com.android.systemui.SystemUIApplication", lpparam.classLoader, "onCreate", new MethodHook() {
            private boolean isHooked = false;

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (!isHooked) {
                    isHooked = true;
                    Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    Resources res = mContext.getResources();
                    float density = res.getDisplayMetrics().density;
                    int tileWidthResId = res.getIdentifier("qs_control_center_tile_width", "dimen", "com.android.systemui");
                    float tileWidthDim = res.getDimension(tileWidthResId);
                    if (cols > 4) {
                        tileWidthDim = tileWidthDim / density;
                        scaledTileWidthDim = tileWidthDim * 4 / cols;
                        mResHook.setDensityReplacement(lpparam.packageName, "dimen", "qs_control_center_tile_width", scaledTileWidthDim);
                        mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_control_center_tile_width", scaledTileWidthDim);
                        mResHook.setDensityReplacement(lpparam.packageName, "dimen", "qs_control_tile_icon_bg_size", scaledTileWidthDim);
                        mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_control_tile_icon_bg_size", scaledTileWidthDim);
                        mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85f);
                    }
                }
            }
        });

        String pluginLoaderClass = isAndroidMoreVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.shared.plugins.PluginInstance$Factory" : "com.android.systemui.shared.plugins.PluginManagerImpl";
        Helpers.hookAllMethods(pluginLoaderClass, lpparam.classLoader, "getClassLoader", new MethodHook() {
            private boolean isHooked = false;

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked) {
                    isHooked = true;
                    if (pluginLoader == null) {
                        pluginLoader = (ClassLoader) param.getResult();
                    }
                    if (cols > 4) {
                        Helpers.findAndHookConstructor("miui.systemui.controlcenter.qs.QSPager", pluginLoader, Context.class, AttributeSet.class, new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) throws Throwable {
                                XposedHelpers.setObjectField(param.thisObject, "columns", cols);
                            }
                        });
                        if (!mPrefsMap.getBoolean("system_control_center_qs_tile_label")) {
                            Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.StandardTileView", pluginLoader, "createLabel", boolean.class, new MethodHook() {
                                @Override
                                protected void after(MethodHookParam param) throws Throwable {
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
                            });
                        }
                    }
                    if (rows != 4) {
                        Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.QSPager", pluginLoader, "distributeTiles", new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) throws Throwable {
                                boolean collapse = (boolean) XposedHelpers.getObjectField(param.thisObject, "collapse");
                                if (collapse) {
                                    ArrayList<Object> pages = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "pages");
                                    for (Object tileLayoutImpl : pages) {
                                        XposedHelpers.callMethod(tileLayoutImpl, "removeTiles");
                                    }
                                    ArrayList<Object> pageTiles = new ArrayList<Object>();
                                    int currentRow = 2;
                                    ArrayList<?> records = (ArrayList<?>) XposedHelpers.getObjectField(param.thisObject, "records");
                                    Iterator<?> it2 = records.iterator();
                                    int i3 = 0;
                                    int pageNow = 0;
                                    Object bigHeader = XposedHelpers.getObjectField(param.thisObject, "header");
                                    while (it2.hasNext()) {
                                        Object tileRecord = it2.next();
                                        pageTiles.add(tileRecord);
                                        i3++;
                                        if (i3 >= cols) {
                                            currentRow++;
                                            i3 = 0;
                                        }
                                        if (currentRow >= rows || !it2.hasNext()) {
                                            XposedHelpers.callMethod(pages.get(pageNow), "setTiles", pageTiles, pageNow == 0 ? bigHeader : null);
                                            pageTiles.clear();
                                            int totalRows = (int) XposedHelpers.getObjectField(param.thisObject, "rows");
                                            if (currentRow > totalRows) {
                                                XposedHelpers.setObjectField(param.thisObject, "rows", currentRow);
                                            }
                                            if (it2.hasNext()) {
                                                pageNow++;
                                                currentRow = 0;
                                            }
                                        }
                                    }
                                    Iterator<Object> it3 = pages.iterator();
                                    while (it3.hasNext()) {
                                        Object next2 = it3.next();
                                        boolean isEmpty = (boolean) XposedHelpers.callMethod(next2, "isEmpty");
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
//                                    XposedHelpers.callMethod(param.thisObject, "notifyDataSetChanged");
                                }
                            }
                        });
                    }
                    // 移除磁贴标题相关
                    if (mPrefsMap.getBoolean("system_control_center_qs_tile_label")) {
                        HideCCLabels(pluginLoader);
                    }

                    // 新控制中心矩形圆角
                    if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect")) {
                        mResHook.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_unavailable", R.drawable.ic_qs_tile_bg_disabled);
                        mResHook.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_disabled", R.drawable.ic_qs_tile_bg_disabled);
                        mResHook.setResReplacement("miui.systemui.plugin", "drawable", "qs_background_warning", R.drawable.ic_qs_tile_bg_warning);
                        CCTileCornerHook(pluginLoader);
                    }
                }
            }

            private void HideCCLabels(ClassLoader pluginLoader) {
                mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85f);
                Class<?> QSController = XposedHelpers.findClassIfExists("miui.systemui.controlcenter.qs.tileview.StandardTileView", pluginLoader);
                Helpers.hookAllMethods(QSController, "init", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        if (param.args.length != 1) return;
                        View mLabelContainer = (View) XposedHelpers.getObjectField(param.thisObject, "labelContainer");
                        if (mLabelContainer != null) {
                            mLabelContainer.setVisibility(View.GONE);
                        }
                    }
                });
            }

            private void CCTileCornerHook(ClassLoader pluginLoader) {
                Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.tileview.ExpandableIconView", pluginLoader, "setCornerRadius", float.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        Context mContext = (Context) XposedHelpers.callMethod(param.thisObject, "getPluginContext");
                        float radius = 18;
                        if (scaledTileWidthDim > 0) {
                            radius *= scaledTileWidthDim / 65;
                        }
                        param.args[0] = mContext.getResources().getDisplayMetrics().density * radius;
                    }
                });

                Helpers.findAndHookMethod("miui.systemui.dagger.PluginComponentFactory", pluginLoader, "create", Context.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        Context mContext = (Context) param.args[0];
                        Resources res = mContext.getResources();
                        int enabledTileBackgroundResId = res.getIdentifier("qs_background_enabled", "drawable", "miui.systemui.plugin");
                        int enabledTileColorResId = res.getIdentifier("qs_enabled_color", "color", "miui.systemui.plugin");
                        int tintColor = res.getColor(enabledTileColorResId, null);
                        MethodHook imgHook = new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                int resId = (int) param.args[0];
                                if (resId == enabledTileBackgroundResId && resId != 0) {
                                    Drawable enableTile = Helpers.getModuleRes(mContext).getDrawable(R.drawable.ic_qs_tile_bg_enabled, null);
                                    enableTile.setTint(tintColor);
                                    param.setResult(enableTile);
                                }
                            }
                        };
                        Helpers.findAndHookMethod("android.content.res.Resources", pluginLoader, "getDrawable", int.class, imgHook);
                        Helpers.findAndHookMethod("android.content.res.Resources.Theme", pluginLoader, "getDrawable", int.class, imgHook);
                    }
                });
            }
        });
    }
}
