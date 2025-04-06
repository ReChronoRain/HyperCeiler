package com.sevtinge.hyperceiler.common.utils;

import android.content.Context;

public class SettingsFeatures {

    public static boolean isSplitTabletDevice() {
        return false;
    }

    public static boolean isSplitTablet(Context context) {
        return isSplitTabletDevice();
    }
}
