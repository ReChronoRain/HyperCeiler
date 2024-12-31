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
package com.sevtinge.hyperceiler.utils.devicesdk;

import android.content.Context;
import android.util.DisplayMetrics;

import com.github.kyuubiran.ezxhelper.EzXHelper;

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

    public static int dp2px(float dipValue) {
        final float scale = EzXHelper.getAppContext().getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int sp2px(float spValue) {
        final float scale = EzXHelper.getAppContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * scale + 0.5f);
    }

    public static int sp2px(Context context, float spValue) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * scale + 0.5f);
    }

    public static int px2dp(float pxValue) {
        final float scale = EzXHelper.getAppContext().getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
