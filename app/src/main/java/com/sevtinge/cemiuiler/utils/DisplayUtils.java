package com.sevtinge.cemiuiler.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

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
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(mDisplayMetrics);
        mWidthPixels = mDisplayMetrics.widthPixels;
        mHeightPixels = mDisplayMetrics.heightPixels;
        mDensity = mDisplayMetrics.density;
        mDensityDpi = mDisplayMetrics.densityDpi;
        float f = mDensity;
        mWidthDps = (int) ((float) mWidthPixels / f);
        mHeightDps = (int) ((float) mHeightPixels / f);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
