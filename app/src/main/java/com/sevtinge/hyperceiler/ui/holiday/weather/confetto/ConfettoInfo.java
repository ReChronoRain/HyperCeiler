package com.sevtinge.hyperceiler.ui.holiday.weather.confetto;

import android.graphics.Bitmap;

import com.sevtinge.hyperceiler.ui.holiday.weather.PrecipType;

public final class ConfettoInfo {
    private Bitmap mCustomBitmap;
    private PrecipType mPrecipType;
    private float mScaleFactor;

    public ConfettoInfo(PrecipType type) {
        this(type, 0f);
    }

    public ConfettoInfo(PrecipType type, float scale) {
        this(type, scale, null);
    }

    public ConfettoInfo(PrecipType type, float scale, Bitmap bitmap) {
        mPrecipType = type;
        mScaleFactor = scale;
        mCustomBitmap = bitmap;
    }

    public ConfettoInfo(PrecipType type, float f, Bitmap bitmap, int i) {
        this(type, f, (i & 4) != 0 ? null : bitmap);
    }

    public Bitmap getCustomBitmap() {
        return mCustomBitmap;
    }

    public void setCustomBitmap(Bitmap bitmap) {
        mCustomBitmap = bitmap;
    }

    public PrecipType getPrecipType() {
        return mPrecipType;
    }

    public void setPrecipType(PrecipType type) {
        mPrecipType = type;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public void setScaleFactor(float scale) {
        mScaleFactor = scale;
    }
}
