package com.sevtinge.cemiuiler.module.systemui.controlcenter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class QSTileLabel extends BaseHook {

    private ClassLoader mPluginLoader = null;

    @Override
    public void init() {

        hookAllMethods("com.android.systemui.qs.MiuiTileLayout", "addTile", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                updateLabelsVisibility(param.args[0], XposedHelpers.getIntField(param.thisObject, "mRows"), ((ViewGroup)param.thisObject).getResources().getConfiguration().orientation);
            }
        });

        hookAllMethods("com.android.systemui.qs.MiuiPagedTileLayout", "addTile", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                ArrayList<Object> mPages = (ArrayList<Object>)XposedHelpers.getObjectField(param.thisObject, "mPages");
                if (mPages == null) return;
                int mRows = 0;
                if (mPages.size() > 0) mRows = XposedHelpers.getIntField(mPages.get(0), "mRows");
                updateLabelsVisibility(param.args[0], mRows, ((ViewGroup)param.thisObject).getResources().getConfiguration().orientation);
            }
        });

        findAndHookMethod("com.android.systemui.qs.MiuiTileLayout", "updateResources", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (!(boolean)param.getResult()) return;
                Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
                if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) return;
                XposedHelpers.setIntField(param.thisObject, "mCellHeight", Math.round(XposedHelpers.getIntField(param.thisObject, "mCellHeight") / 1.5f));
                ((ViewGroup)param.thisObject).requestLayout();
            }
        });

        findAndHookMethod("com.android.systemui.qs.tileimpl.MiuiQSTileView", "createLabel", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ViewGroup mLabelContainer = (ViewGroup)XposedHelpers.getObjectField(param.thisObject, "mLabelContainer");
                if (mLabelContainer != null) {
                    mLabelContainer.setPadding(
                            mLabelContainer.getPaddingLeft(),
                            Math.round(mLabelContainer.getResources().getDisplayMetrics().density * 2),
                            mLabelContainer.getPaddingRight(),
                            mLabelContainer.getPaddingBottom()
                    );
                }
            }
        });


        final boolean[] isHooked = {false};

        findAndHookMethod("com.android.systemui.shared.plugins.PluginManagerImpl", "getClassLoader", ApplicationInfo.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked[0]) {
                    isHooked[0] = true;
                    if (mPluginLoader == null) {
                        mPluginLoader = (ClassLoader) param.getResult();
                    }
                    mResHook.setDensityReplacement("miui.systemui.plugin", "dimen", "qs_cell_height", 85.0f);
                    Class<?> mQSController = XposedHelpers.findClassIfExists("miui.systemui.controlcenter.qs.tileview.StandardTileView", mPluginLoader);
                    hookAllMethods(mQSController, "init", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            if (param.args.length != 1) return;
                            View mLabelContainer = (View)XposedHelpers.getObjectField(param.thisObject, "labelContainer");
                            if (mLabelContainer != null) {
                                mLabelContainer.setVisibility(mPrefsMap.getBoolean("system_control_center_qs_tile_label") ? View.GONE : View.VISIBLE);
                            }
                        }
                    });
                }
            }
        });
    }


    private void updateLabelsVisibility(Object mRecord, int mRows, int orientation) {
        if (mRecord == null) return;
        Object tileView = XposedHelpers.getObjectField(mRecord, "tileView");
        if (tileView != null) {
            ViewGroup mLabelContainer = null;
            try {
                mLabelContainer = (ViewGroup)XposedHelpers.getObjectField(tileView, "mLabelContainer");
            }
            catch (Throwable ignore) {}

            if (mLabelContainer != null) {
                mLabelContainer.setVisibility(mPrefsMap.getBoolean("system_control_center_qs_tile_label") ||
                                orientation == Configuration.ORIENTATION_PORTRAIT && mRows >= 5 ||
                                orientation == Configuration.ORIENTATION_LANDSCAPE && mRows >= 3 ? View.GONE : View.VISIBLE
                );
            }
        }
    }
}
