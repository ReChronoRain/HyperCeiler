package com.sevtinge.hyperceiler.utils.api;

import com.sevtinge.hyperceiler.BuildConfig;

public class AppApi {
    public static String getBuildType() {
        switch (BuildConfig.BUILD_TYPE) {
            case "release" -> {
                return "release";
            }
            case "beta" -> {
                return "beta";
            }
            case "canary" -> {
                return "canary";
            }
            case "debug" -> {
                return "debug";
            }
            default -> {
                return "unknown";
            }
        }
    }

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
