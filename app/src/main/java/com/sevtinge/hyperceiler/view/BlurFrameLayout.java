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
package com.sevtinge.hyperceiler.view;

import static com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils;

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

            mBlurRadius = mPrefsMap.getInt(mBlurRadiusKey, 60);

            mBgColor = mPrefsMap.getInt(mBgColorKey, -1);
            mBgAlpha = mPrefsMap.getInt(mBgAlphaKey, 60);
            mBgCornerRadius = DisplayUtils.dp2px(mPrefsMap.getInt(mBgCornerRadiusKey, 90));
        }
        setOnAttachStateChangeListener(mBlurView);
    }

    private void setOnAttachStateChangeListener(View view) {
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                mViewRootImpl = XposedHelpers.callMethod(v, "getViewRootImpl");
                mBlurDrawable = (Drawable) XposedHelpers.callMethod(mViewRootImpl, "createBackgroundBlurDrawable", new Object[0]);
                setBackgroundDrawable(mContext, v, isBlurEnable, mBgColor, mBgAlpha, mBgCornerRadius, mBlurRadius);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
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
        mBackgroundDrawable.setCornerRadius(DisplayUtils.dp2px(cornerRadius));
        return mBackgroundDrawable;
    }
}
