package com.sevtinge.hyperceiler.utils;

import android.content.Context;

public class SettingsFeatures {

    public static boolean isSplitTabletDevice() {
        return false;
    }

    public static boolean isSplitTablet(Context context) {
        return isSplitTabletDevice();
    }
}
