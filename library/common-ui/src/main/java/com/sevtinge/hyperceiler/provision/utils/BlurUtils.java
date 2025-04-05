package com.sevtinge.hyperceiler.provision.utils;

import android.view.View;

import fan.core.utils.MiuiBlurUtils;

public class BlurUtils {

    public static void setupLogoBlur(View view) {
        setupViewBlur(view, new int[]{-867546550, -11579569, -15011328}, new int[]{19, 100, 106});
    }

    public static void setupViewBlur(View view, int[] blendColors, int[] iArr2) {
        setupViewBlur(view, true, blendColors, iArr2);
    }

    public static void setupViewBlur(View view, boolean isEnabled, int[] blendColors, int[] iArr2) {
        if (view != null) {
            if (isEnabled) {
                MiuiBlurUtils.setViewBlurMode(view, 3);
                for (int i = 0; i < blendColors.length; i++) {
                    MiuiBlurUtils.addBackgroundBlenderColor(view, blendColors[i], iArr2[i]);
                }
            } else {
                MiuiBlurUtils.setViewBlurMode(view, 0);
                MiuiBlurUtils.clearBackgroundBlenderColor(view);
            }
        }
    }
}
