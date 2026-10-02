package com.sevtinge.hyperceiler.common.utils;

public final class PrefsBridge {
    private PrefsBridge() { }
    private static java.util.Map<String, Integer> values = java.util.Map.of();
    public static void setTestValues(java.util.Map<String, Integer> source) {
        values = java.util.Map.copyOf(source);
    }
    private static String normalized(String key) {
        return key.startsWith("prefs_key_") ? key.substring("prefs_key_".length()) : key;
    }
    public static boolean getBoolean(String key, boolean fallback) {
        return values.containsKey(normalized(key)) ? values.get(normalized(key)) != 0 : fallback;
    }
    public static int getStringAsInt(String key, int fallback) { return getInt(key, fallback); }
    public static int getInt(String key, int fallback) { return values.getOrDefault(normalized(key), fallback); }
}
