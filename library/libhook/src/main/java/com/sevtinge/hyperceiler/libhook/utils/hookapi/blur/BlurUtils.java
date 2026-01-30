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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.blur;

import static com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

public class BlurUtils {
    private final Context mContext;
    private Object mViewRootImpl;
    private Drawable mBlurDrawable;

    private boolean isEnable;

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
        if (isEnable) setBlurView(view);
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
            Object mViewRootImpl = EzxHelpUtils.callMethod(view, "getViewRootImpl");
            if (mViewRootImpl == null) return null;
            Drawable blurDrawable = (Drawable) EzxHelpUtils.callMethod(mViewRootImpl, "createBackgroundBlurDrawable");
            EzxHelpUtils.callMethod(blurDrawable, "setBlurRadius", blurRadius);
            EzxHelpUtils.callMethod(blurDrawable, "setCornerRadius", cornerRadius);
            if (color != -1) EzxHelpUtils.callMethod(blurDrawable, "setColor", color);
            return blurDrawable;
        } catch (Throwable e) {
            AndroidLog.e("createBlurDrawable", "Create BlurDrawable Error" + e);
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

            String mCustomBackgroundEnabledKey = key + "_custom_enable";

            String mBlurEnableKey = key + "_blur_enabled";
            String mBlurRadiusKey = key + "_blur_radius";

            String mColorKey = key + "_color";
            String mAlphaKey = key + "_color_alpha";
            String mCornerRadiusKey = key + "_corner_radius";

            isEnable = mPrefsMap.getBoolean(mCustomBackgroundEnabledKey);

            isBlurEnable = mPrefsMap.getBoolean(mBlurEnableKey);
            mBlurRadius = mPrefsMap.getInt(mBlurRadiusKey, 60);

            mColor = mPrefsMap.getInt(mColorKey, 2113929215);
            mAlpha = mPrefsMap.getInt(mAlphaKey, 60);
            mCornerRadius = DisplayUtils.dp2px(context, mPrefsMap.getInt(mCornerRadiusKey, 18));

        } else {
            isEnable = false;

            isBlurEnable = false;
            mBlurRadius = 60;

            mColor = 2113929215;
            mAlpha = 60;
            mCornerRadius = DisplayUtils.dp2px(context, 90);
        }
    }


    private void setOnAttachStateChangeListener(View view) {
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                mViewRootImpl = EzxHelpUtils.callMethod(v, "getViewRootImpl");
                mBlurDrawable = createBackgroundDrawable(mViewRootImpl, isBlurEnable, mColor, mCornerRadius, mBlurRadius);
                v.setBackground(mBlurDrawable);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                v.setBackground(null);
            }
        });
    }

    private Drawable createBackgroundDrawable(Object viewRootImpl, boolean isBlurEnable, int color, int cornerRadius, int blurRadius) {
        Drawable mBackgroundDrawable;
        if (isBlurEnable) {
            mBackgroundDrawable = (Drawable) EzxHelpUtils.callMethod(viewRootImpl, "createBackgroundBlurDrawable");
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

    public void setColor(Drawable drawable, int color) {
        int mColorAlpha = (color & 0xff000000) >> 24;
        int mColorRed = (color & 0x00ff0000) >> 16;
        int mColorGreen = (color & 0x0000ff00) >> 8;
        int mColorBlue = (color & 0x000000ff);
        EzxHelpUtils.callMethod(drawable, "setColor", Color.argb(mColorAlpha, mColorRed, mColorGreen, mColorBlue));
    }

    public void setCornerRadius(Drawable drawable, int cornerRadius) {
        EzxHelpUtils.callMethod(drawable, "setCornerRadius", cornerRadius);
    }

    public void setBlurRadius(Drawable drawable, int blurRadius) {
        EzxHelpUtils.callMethod(drawable, "setBlurRadius", blurRadius);
    }
}
