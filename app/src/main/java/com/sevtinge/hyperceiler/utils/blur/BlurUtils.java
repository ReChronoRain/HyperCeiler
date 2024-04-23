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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils.blur;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logW;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.color.ColorUtilsStatic;
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class BlurUtils {
    private final Context mContext;
    private Object mViewRootImpl;
    private Drawable mBlurDrawable;

    private int mColor;
    private int mAlpha;
    private int mCornerRadius;

    private int mBlurRadius;
    private boolean isBlurEnable;

    public BlurUtils(View view) {
        this(view, "");
    }

    public BlurUtils(View view, String key) {
        mContext = view.getContext();
        setKey(mContext, key);
        setBlurView(view);
    }

    public void setBlurView(View view) {
        setOnAttachStateChangeListener(view);
    }

    public void setBlurEnable(boolean blurEnable) {
        isBlurEnable = blurEnable;
    }

    public static Drawable createBlurDrawable(View view, int blurRadius, int cornerRadius) {
        return createBlurDrawable(view, blurRadius, cornerRadius, -1);
    }

    public static Drawable createBlurDrawable(View view, int blurRadius, int cornerRadius, int color) {
        try {
            Object mViewRootImpl = XposedHelpers.callMethod(view, "getViewRootImpl");
            if (mViewRootImpl == null) return null;
            Drawable blurDrawable = (Drawable) XposedHelpers.callMethod(mViewRootImpl, "createBackgroundBlurDrawable");
            XposedHelpers.callMethod(blurDrawable, "setBlurRadius", blurRadius);
            XposedHelpers.callMethod(blurDrawable, "setCornerRadius", cornerRadius);
            if (color != -1) XposedHelpers.callMethod(blurDrawable, "setColor", color);
            return blurDrawable;
        } catch (Throwable e) {
            logW("createBlurDrawable", "Create BlurDrawable Error", e);
            return null;
        }
    }

    public static boolean isBlurDrawable(Drawable drawable) {
        // 不够严谨，可以用
        if (drawable == null) {
            return false;
        }
        String drawableClassName = drawable.getClass().getName();
        return drawableClassName.contains("BackgroundBlurDrawable");
    }

    public void setKey(Context context, String key) {
        if (!TextUtils.isEmpty(key)) {

            String mBlurEnableKey = key + "_blur_enabled";
            String mBlurRadiusKey = key + "_blur_radius";

            String mColorKey = key + "_color";
            String mAlphaKey = key + "_color_alpha";
            String mCornerRadiusKey = key + "_corner_radius";

            isBlurEnable = XposedInit.mPrefsMap.getBoolean(mBlurEnableKey);
            mBlurRadius = XposedInit.mPrefsMap.getInt(mBlurRadiusKey, 60);

            mColor = XposedInit.mPrefsMap.getInt(mColorKey, 2113929215);
            mAlpha = XposedInit.mPrefsMap.getInt(mAlphaKey, 60);
            mCornerRadius = DisplayUtils.dp2px(XposedInit.mPrefsMap.getInt(mCornerRadiusKey, 18));

        } else {
            isBlurEnable = false;
            mBlurRadius = 60;

            mColor = 2113929215;
            mAlpha = 60;
            mCornerRadius = DisplayUtils.dp2px(90);
        }
    }


    private void setOnAttachStateChangeListener(View view) {
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                mViewRootImpl = XposedHelpers.callMethod(v, "getViewRootImpl");
                mBlurDrawable = createBackgroundDrawable(mViewRootImpl, isBlurEnable, ColorUtilsStatic.colorToHexARGB(mColor), mCornerRadius, mBlurRadius);
                v.setBackground(mBlurDrawable);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.setBackground(null);
            }
        });
    }

    private Drawable createBackgroundDrawable(Object viewRootImpl, boolean isBlurEnable, int color, int cornerRadius, int blurRadius) {
        Drawable mBackgroundDrawable;
        if (isBlurEnable) {
            mBackgroundDrawable = (Drawable) XposedHelpers.callMethod(viewRootImpl, "createBackgroundBlurDrawable", new Object[0]);
            setColor(mBackgroundDrawable, color);
            setCornerRadius(mBackgroundDrawable, cornerRadius);
            setBlurRadius(mBackgroundDrawable, blurRadius);
        } else {
            mBackgroundDrawable = createGradientDrawable(color, cornerRadius);
        }
        return mBackgroundDrawable;
    }

    private Drawable createBackgroundDrawable(Object viewRootImpl, boolean isBlurEnable, String color, int cornerRadius, int blurRadius) {
        Drawable mBackgroundDrawable;
        if (isBlurEnable) {
            mBackgroundDrawable = (Drawable) XposedHelpers.callMethod(viewRootImpl, "createBackgroundBlurDrawable", new Object[0]);
            setColor(mBackgroundDrawable, color);
            setCornerRadius(mBackgroundDrawable, cornerRadius);
            setBlurRadius(mBackgroundDrawable, blurRadius);
        } else {
            mBackgroundDrawable = createGradientDrawable(color, cornerRadius);
        }
        return mBackgroundDrawable;
    }

    private GradientDrawable createGradientDrawable(int color, int cornerRadius) {

        int mColorAlpha = (color & 0xff000000) >> 24;
        int mColorRed = (color & 0x00ff0000) >> 16;
        int mColorGreen = (color & 0x0000ff00) >> 8;
        int mColorBlue = (color & 0x000000ff);

        GradientDrawable mBackgroundDrawable = new GradientDrawable();
        mBackgroundDrawable.setShape(GradientDrawable.RECTANGLE);
        mBackgroundDrawable.setColor(Color.argb(mColorAlpha, mColorRed, mColorGreen, mColorBlue));
        mBackgroundDrawable.setCornerRadius(cornerRadius);
        return mBackgroundDrawable;
    }

    private GradientDrawable createGradientDrawable(String color, int cornerRadius) {
        GradientDrawable mBackgroundDrawable = new GradientDrawable();
        mBackgroundDrawable.setShape(GradientDrawable.RECTANGLE);
        mBackgroundDrawable.setColor(Color.parseColor(color));
        mBackgroundDrawable.setCornerRadius(cornerRadius);
        return mBackgroundDrawable;
    }

    public void setColor(Drawable drawable, int color) {
        int mColorAlpha = (color & 0xff000000) >> 24;
        int mColorRed = (color & 0x00ff0000) >> 16;
        int mColorGreen = (color & 0x0000ff00) >> 8;
        int mColorBlue = (color & 0x000000ff);
        XposedHelpers.callMethod(drawable, "setColor", Color.argb(mColorAlpha, mColorRed, mColorGreen, mColorBlue));
    }

    public void setColor(Drawable drawable, String color) {
        XposedHelpers.callMethod(drawable, "setColor", Color.parseColor(color));
    }

    public void setCornerRadius(Drawable drawable, int cornerRadius) {
        XposedHelpers.callMethod(drawable, "setCornerRadius", new Object[]{cornerRadius});
    }

    public void setBlurRadius(Drawable drawable, int blurRadius) {
        XposedHelpers.callMethod(drawable, "setBlurRadius", new Object[]{blurRadius});
    }


    /*public BlurUtils(View view) {
        this(view, null);
    }

    public BlurUtils(View view, String key) {
        mView = view;
        mKey = key;
        if (mView != null) {
            mContext = mView.getContext();
        }
        if (mKey != null) {
            mBlurRadiusKey = mKey + "_blur_radius";
            mBgCornerRadiusKey = mKey + "_bg_corner_radius";
            mBgAlphaKey = mKey + "_bg_alpha";
            mBgColorKey = mKey + "_bg_color";

            mBlurRadius = XposedInit.mPrefsMap.getInt(mBlurRadiusKey,60);
            mBgCornerRadius = DisplayUtils.dip2px(mContext, XposedInit.mPrefsMap.getInt(mBgCornerRadiusKey, 90));
            mBgAlpha = XposedInit.mPrefsMap.getInt(mBgAlphaKey,60);
            mBgColor = XposedInit.mPrefsMap.getInt(mBgColorKey,-1);
        }
        setBlur(mView);
    }

    void setBlur(View view) {

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                onAttachedToWindow();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                onDetachedFromWindow();
            }
        });
    }

    void onAttachedToWindow() {
        mViewRootImpl = XposedHelpers.callMethod(mView,"getViewRootImpl",new Object[0]);
        mBlurDrawable = (Drawable) XposedHelpers.callMethod(mViewRootImpl,"createBackgroundBlurDrawable",new Object[0]);
        initBlur();
    }

    void onDetachedFromWindow() {
        mView.setBackground(null);
    }

    void initBlur() {
        setBlurRadius();
        setCornerRadius();
        setColor();
        setBlurBackground();
    }

    void setBlurBackground() {
        mView.setBackground(mBlurDrawable);
    }

    void setBlurRadius() {
        XposedHelpers.callMethod(mBlurDrawable,"setBlurRadius",new Object[]{mBlurRadius});
    }

    void setCornerRadius() {
        XposedHelpers.callMethod(mBlurDrawable,"setCornerRadius",new Object[]{mBgCornerRadius});
    }

    private static void setColor() {
        int mColorRed = (mBgColor & 0x00ff0000) >> 16;
        int mColorGreen = (mBgColor & 0x0000ff00) >> 8;
        int mColorBlue = (mBgColor & 0x000000ff);
        XposedHelpers.callMethod(mBlurDrawable,"setColor", Color.argb(mBgAlpha, mColorRed, mColorGreen, mColorBlue));
    }*/
}
