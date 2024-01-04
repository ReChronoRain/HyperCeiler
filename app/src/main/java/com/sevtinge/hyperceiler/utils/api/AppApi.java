package com.sevtinge.hyperceiler.utils.api;

import com.sevtinge.hyperceiler.BuildConfig;

public class AppApi {
    public static boolean isRelease() {
        return "release".equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isBeta() {
        return "beta".equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isCanary() {
        return "canary".equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isDebug() {
        return "debug".equals(BuildConfig.BUILD_TYPE);
    }
}
