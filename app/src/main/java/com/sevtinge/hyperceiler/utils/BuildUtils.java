package com.sevtinge.hyperceiler.utils;

import com.sevtinge.hyperceiler.BuildConfig;

public class BuildUtils {
    public static String getBuildType() {
        if (BuildConfig.BUILD_TYPE.contains("debug")) return "debug";
        else if (BuildConfig.BUILD_TYPE.contains("canary")) return "canary";
        else if (BuildConfig.BUILD_TYPE.contains("beta")) return "beta";
        else if (BuildConfig.BUILD_TYPE.contains("release")) return "release";
        else return "unknown";
    }
}
