package com.sevtinge.hyperceiler.common.utils;

import java.util.HashMap;
import java.util.Map;

public final class PrefsBridge {
    public static final Map<String, String> values = new HashMap<>();
    public static int getStringAsInt(String key, int fallback) {
        String full = key.startsWith("prefs_key_") ? key : "prefs_key_" + key;
        try { return Integer.parseInt(values.get(full)); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
