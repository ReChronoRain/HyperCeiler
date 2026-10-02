package com.sevtinge.hyperceiler.libhook.base;
public class BaseHook {
    private static final java.util.Map<String, Object> state = new java.util.HashMap<>();
    public static void putHotReloadRuntimeState(String key, Object value) { state.put(key, value); }
    public static <T> T getHotReloadRuntimeState(String key, Class<T> type) {
        Object value = state.get(key); return type.isInstance(value) ? type.cast(value) : null;
    }
    public static void resetTestState() { state.clear(); }
}
