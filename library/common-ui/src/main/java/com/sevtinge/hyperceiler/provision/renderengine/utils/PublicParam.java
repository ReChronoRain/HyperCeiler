package com.sevtinge.hyperceiler.provision.renderengine.utils;

import android.content.Context;

public class PublicParam {
    private static Context mContext;
    private static final float[] mResolution = new float[2];

    public static void setContext(Context context) {
        mContext = context;
    }

    public static Context getContext() {
        return mContext;
    }

    public static void setResolution(int i, int i2) {
        float[] fArr = mResolution;
        fArr[0] = i;
        fArr[1] = i2;
    }

    public static float[] getResolution() {
        return mResolution;
    }
}
