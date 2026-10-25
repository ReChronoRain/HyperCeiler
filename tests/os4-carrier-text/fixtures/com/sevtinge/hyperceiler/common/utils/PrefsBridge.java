package com.sevtinge.hyperceiler.common.utils;

import java.util.HashMap;
import java.util.Map;

public class PrefsBridge {
    public static int mode;
    public static final Map<String, String> strings = new HashMap<>();

    public static int getStringAsInt(String k, int d) {
        if (!k.equals("system_ui_control_center_hide_operator")) throw new AssertionError(k);
        return mode;
    }

    public static String getString(String k, String d) {
        String v = strings.get(k);
        return v == null ? d : v;
    }
}
