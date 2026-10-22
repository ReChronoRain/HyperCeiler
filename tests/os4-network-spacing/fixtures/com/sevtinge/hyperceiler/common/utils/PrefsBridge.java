package com.sevtinge.hyperceiler.common.utils;
public class PrefsBridge {
    public static int value = 40;
    public static boolean fail;
    public static Runnable onRead;
    public static int getInt(String key, int fallback) {
        if (!key.equals("system_ui_statusbar_network_speed_update_spacings")) throw new AssertionError(key);
        if (fail) throw new IllegalStateException("injected preference failure");
        if (onRead!=null) { Runnable action=onRead; onRead=null; action.run(); }
        return value;
    }
}
