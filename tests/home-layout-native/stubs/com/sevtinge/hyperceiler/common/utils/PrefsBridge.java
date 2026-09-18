package com.sevtinge.hyperceiler.common.utils;

public final class PrefsBridge {
    private PrefsBridge() { }
    public static boolean getBoolean(String key, boolean fallback) { return fallback; }
    public static int getInt(String key, int fallback) { return fallback; }
}
