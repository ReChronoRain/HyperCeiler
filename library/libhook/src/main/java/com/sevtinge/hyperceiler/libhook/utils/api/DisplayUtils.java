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
package com.sevtinge.hyperceiler.libhook.utils.api;

import static io.github.kyuubiran.ezxhelper.xposed.EzXposed.getAppContext;

import android.content.Context;
import android.util.DisplayMetrics;
import com.sevtinge.hyperceiler.common.log.XposedLog;

/**
 * 显示工具类
 * 提供 dp/sp/px 单位转换功能
 */
public class DisplayUtils {

    public static float mDensity;
    public static int mDensityDpi;
    public static DisplayMetrics mDisplayMetrics;
    public static int mHeightDps;
    public static int mHeightPixels;
    public static int mWidthDps;
    public static int mWidthPixels;

    public static void getAndroidScreenProperty(Context context) {
        mDisplayMetrics = new DisplayMetrics();
        context.getDisplay().getMetrics(mDisplayMetrics);
        mWidthPixels = mDisplayMetrics.widthPixels;
        mHeightPixels = mDisplayMetrics.heightPixels;
        mDensity = mDisplayMetrics.density;
        mDensityDpi = mDisplayMetrics.densityDpi;
        float f = mDensity;
        mWidthDps = (int) ((float) mWidthPixels / f);
        mHeightDps = (int) ((float) mHeightPixels / f);
    }

    public static int dp2px(int dipValue) {
        return dp2px((float) dipValue);
    }

    public static int dp2px(float dipValue) {
        try {
            final float scale = getAppContext().getResources().getDisplayMetrics().density;
            /* XposedLog.d(
                "DisplayUtils",
                "Dip: " + dipValue + ", Density: " + scale + ", Px: " + ((int) (dipValue * scale + 0.5f))); */
            return (int) (dipValue * scale + 0.5f);
        } catch (Throwable t) {
            XposedLog.e(
                "dp2px",
                "DisplayUtils",
                "Error getting density",
                t);
        }
        return (int) dipValue;
    }

    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int sp2px(float spValue) {
        try {
            final float scale = getAppContext().getResources().getDisplayMetrics().scaledDensity;
            /*  XposedLog.d(
                "DisplayUtils",
                "Sp: " + spValue + ", ScaledDensity: " + scale + ", Px: " + ((int) (spValue * scale + 0.5f))); */
            return (int) (spValue * scale + 0.5f);
        } catch (Throwable t) {
            XposedLog.e(
                "sp2px",
                "DisplayUtils",
                "Error getting scaled density",
                t);
        }
        return (int) spValue;
    }

    public static int sp2px(Context context, float spValue) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * scale + 0.5f);
    }

    public static int px2dp(float pxValue) {
        try {
            final float scale = getAppContext().getResources().getDisplayMetrics().density;
            /*  XposedLog.d(
                "DisplayUtils",
                "Px: " + pxValue + ", Density: " + scale + ", Dp: " + ((int) (pxValue / scale + 0.5f))); */
            return (int) (pxValue / scale + 0.5f);
        } catch (Throwable t) {
            XposedLog.e(
                "px2dp",
                "DisplayUtils",
                "Error getting density",
                t);
        }
        return (int) pxValue;
    }
}
