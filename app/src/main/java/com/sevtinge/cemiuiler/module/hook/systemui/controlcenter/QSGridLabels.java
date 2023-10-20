package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class QSGridLabels extends BaseHook {
    @Override
    public void init() {
        Helpers.hookAllMethods("com.android.systemui.qs.MiuiTileLayout", lpparam.classLoader, "addTile", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                updateLabelsVisibility(param.args[0], XposedHelpers.getIntField(param.thisObject, "mRows"), ((ViewGroup) param.thisObject).getResources().getConfiguration().orientation);
            }
        });

        Helpers.hookAllMethods("com.android.systemui.qs.MiuiPagedTileLayout", lpparam.classLoader, "addTile", new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void before(MethodHookParam param) throws Throwable {
                ArrayList<Object> mPages = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mPages");
                if (mPages == null) return;
                int mRows = 0;
                if (mPages.size() > 0) mRows = XposedHelpers.getIntField(mPages.get(0), "mRows");
                updateLabelsVisibility(param.args[0], mRows, ((ViewGroup) param.thisObject).getResources().getConfiguration().orientation);
            }
        });

        int rows = isMoreAndroidVersion(33)
            ? mPrefsMap.getInt("system_control_center_old_qs_rows", 1)
            : mPrefsMap.getInt("system_control_center_old_qs_row", 1);
        if (rows == 4) {
            Helpers.findAndHookMethod("com.android.systemui.qs.tileimpl.MiuiQSTileView", lpparam.classLoader, "createLabel", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    ViewGroup mLabelContainer = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mLabelContainer");
                    if (mLabelContainer != null) mLabelContainer.setPadding(
                        mLabelContainer.getPaddingLeft(),
                        Math.round(mLabelContainer.getResources().getDisplayMetrics().density * 2),
                        mLabelContainer.getPaddingRight(),
                        mLabelContainer.getPaddingBottom()
                    );
                }
            });
        }
    }

    private static void updateLabelsVisibility(Object mRecord, int mRows, int orientation) {
        if (mRecord == null) return;
        Object tileView = XposedHelpers.getObjectField(mRecord, "tileView");
        if (tileView != null) {
            ViewGroup mLabelContainer = null;
            try {
                mLabelContainer = (ViewGroup) XposedHelpers.getObjectField(tileView, "mLabelContainer");
            } catch (Throwable ignore) {
            }

            if (mLabelContainer != null) {
                if (isMoreAndroidVersion(33)) {
                    mLabelContainer.setVisibility(
                        mPrefsMap.getBoolean("system_control_center_qs_tile_label") ? View.GONE : View.VISIBLE
                    );
                } else {
                    mLabelContainer.setVisibility(
                        mPrefsMap.getBoolean("system_control_center_qs_tile_label") ||
                            orientation == Configuration.ORIENTATION_PORTRAIT && mRows >= 5 ||
                            orientation == Configuration.ORIENTATION_LANDSCAPE && mRows >= 3 ? View.GONE : View.VISIBLE
                    );
                }
            }
        }
    }

}
