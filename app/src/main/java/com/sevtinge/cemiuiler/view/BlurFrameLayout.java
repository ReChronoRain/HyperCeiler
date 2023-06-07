package com.sevtinge.cemiuiler.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.utils.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class BlurFrameLayout {

    int mBgColor;
    int mBgAlpha;
    int mBgCornerRadius;
    int mBlurRadius;

    boolean isBlurEnable;

    Context mContext;
    Drawable mBlurDrawable;

    Object mViewRootImpl;
    View mBlurView;


    public BlurFrameLayout(Context context, boolean blurEnable) {
        isBlurEnable = blurEnable;
    }

    public BlurFrameLayout(View view, String key) {
        this(view.getContext(), false);
        mContext = view.getContext();
        mBlurView = new FrameLayout(mContext);
        if (!TextUtils.isEmpty(key)) {
            String mBgColorKey = key + "_bg_color";
            String mBgAlphaKey = key + "_bg_alpha";
            String mBgCornerRadiusKey = key + "_bg_corner_radius";

            String mBlurRadiusKey = key + "_blur_radius";

            mBlurRadius = XposedInit.mPrefsMap.getInt(mBlurRadiusKey, 60);

            mBgColor = XposedInit.mPrefsMap.getInt(mBgColorKey, -1);
            mBgAlpha = XposedInit.mPrefsMap.getInt(mBgAlphaKey, 60);
            mBgCornerRadius = DisplayUtils.dip2px(mContext, XposedInit.mPrefsMap.getInt(mBgCornerRadiusKey, 90));
        }
        setOnAttachStateChangeListener(mBlurView);
    }

    private void setOnAttachStateChangeListener(View view) {
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                mViewRootImpl = XposedHelpers.callMethod(v, "getViewRootImpl");
                mBlurDrawable = (Drawable) XposedHelpers.callMethod(mViewRootImpl, "createBackgroundBlurDrawable", new Object[0]);
                setBackgroundDrawable(mContext, v, isBlurEnable, mBgColor, mBgAlpha, mBgCornerRadius, mBlurRadius);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.setBackground(null);
            }
        });
    }


    private void setBackgroundDrawable(Context context, View view, boolean isBlurEnable, int color, int alpha, int cornerRadius, int blurRadius) {
        if (isBlurEnable) {
            setColor(color, alpha);
            setCornerRadius(cornerRadius);
            setBlurRadius(blurRadius);
        } else {
            view.setBackground(createGradientDrawable(context, color, alpha, cornerRadius));
        }
    }


    public void setColor(int color, int alpha) {
        int mColorRed = (color & 0x00ff0000) >> 16;
        int mColorGreen = (color & 0x0000ff00) >> 8;
        int mColorBlue = (color & 0x000000ff);
        XposedHelpers.callMethod(mBlurDrawable, "setColor", Color.argb(alpha, mColorRed, mColorGreen, mColorBlue));
    }

    public void setCornerRadius(int cornerRadius) {
        XposedHelpers.callMethod(mBlurDrawable, "setCornerRadius", cornerRadius);
    }

    public void setBlurRadius(int blurRadius) {
        XposedHelpers.callMethod(mBlurDrawable, "setBlurRadius", blurRadius);
    }

    private GradientDrawable createGradientDrawable(Context context, int color, int alpha, int cornerRadius) {

        int mColorRed = (color & 0x00ff0000) >> 16;
        int mColorGreen = (color & 0x0000ff00) >> 8;
        int mColorBlue = (color & 0x000000ff);

        GradientDrawable mBackgroundDrawable = new GradientDrawable();
        mBackgroundDrawable.setShape(GradientDrawable.RECTANGLE);
        mBackgroundDrawable.setColor(Color.argb(alpha, mColorRed, mColorGreen, mColorBlue));
        mBackgroundDrawable.setCornerRadius(DisplayUtils.dip2px(context, cornerRadius));
        return mBackgroundDrawable;
    }
}
