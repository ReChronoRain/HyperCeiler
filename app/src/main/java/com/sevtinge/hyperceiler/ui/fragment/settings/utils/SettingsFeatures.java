package com.sevtinge.hyperceiler.ui.fragment.settings.utils;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import fan.os.BuildCompat;

public class SettingsFeatures {

    public static boolean isPadDevice() {
        return BuildCompat.IS_TABLET;
    }

    public static boolean isFoldDevice() {
        return BuildCompat.IS_FOLDABLE || TextUtils.equals(Build.DEVICE, "zizhan");
    }

    public static boolean isScreenLayoutLarge(Context context) {
        if (context == null) {
            return false;
        }
        int screenLayout = context.getResources().getConfiguration().screenLayout & 15;
        return screenLayout == 3 || screenLayout == 4;
    }

    public static boolean isSplitTablet(Context context) {
        return isPadDevice() || (isFoldDevice() && isScreenLayoutLarge(context));
    }
}
