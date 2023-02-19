package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.ArrayList;
import java.util.Iterator;

import de.robv.android.xposed.XposedHelpers;

public class QSGrid extends BaseHook {

    private ClassLoader mPluginLoader = null;
    private float scaledTileWidthDim = -1f;

    @Override
    public void init() {

        final boolean[] isHooked = {false};
        final boolean[] isListened = {false};
        int rows = mPrefsMap.getInt("system_control_center_qs_rows", 4);
        int cols = mPrefsMap.getInt("system_control_center_qs_columns", 4);
        if (cols > 4) {
            mResHook.setObjectReplacement(lpparam.packageName, "dimen", "qs_control_tiles_columns", cols);
        }
        if (rows > 2) {
            mResHook.setObjectReplacement(lpparam.packageName, "dimen", "qs_control_tiles_min_rows", rows);
        }

        findAndHookMethod("com.android.systemui.SystemUIFactory", "createFromConfig", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (!isListened[0]) {
                    isListened[0] = true;
                    Context mContext = (Context) param.args[0];
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
                        mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85.0f);
                    }
                }
            }
        });

        findAndHookMethod("com.android.systemui.shared.plugins.PluginManagerImpl", "getClassLoader", ApplicationInfo.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked[0]) {
                    isHooked[0] = true;
                    if (mPluginLoader == null) {
                        mPluginLoader = (ClassLoader) param.getResult();
                    }
                    if (cols > 4) {
                        Helpers.findAndHookConstructor("miui.systemui.controlcenter.qs.QSPager", mPluginLoader, Context.class, AttributeSet.class, new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) throws Throwable {
                                XposedHelpers.setObjectField(param.thisObject, "columns", cols);
                            }
                        });
                        if (!mPrefsMap.getBoolean("system_control_center_qs_tile_label")) {
                            Helpers.hookAllMethods("miui.systemui.controlcenter.qs.tileview.StandardTileView", mPluginLoader, "handleStateChanged", new MethodHook() {
                                @Override
                                protected void after(MethodHookParam param) throws Throwable {
                                    Object label = XposedHelpers.getObjectField(param.thisObject, "label");
                                    if (label != null) {
                                        TextView lb = (TextView) label;
                                        lb.setMaxLines(1);
                                        lb.setSingleLine(true);
                                        lb.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                                        lb.setMarqueeRepeatLimit(0);
                                    }
                                }
                            });
                        }
                    }
                    if (rows > 2) {
                        Helpers.findAndHookMethod("miui.systemui.controlcenter.qs.QSPager", mPluginLoader, "distributeTiles", new MethodHook() {
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
                }
            }
        });
    }
}
