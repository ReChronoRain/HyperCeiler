/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import java.util.HashMap;
import java.util.Map;

/** Weather settings and hook authorization from one physical preference snapshot. */
public final class WeatherPrefsSnapshot {
    public static final int SCHEMA = 1;
    private static final String[][] KEYS = {
        {"string", "weather_device_level"},
        {"string", "weather_card_display_type"},
        {"boolean", "weather_unlock_blur"},
        {"boolean", "allow_hook"},
        {"boolean", "framework_api_allow_hook"},
        {"text", "framework_check_reason"},
        {"text", "framework_check_name"},
        {"text", "framework_check_version"},
        {"long", "framework_check_version_code"},
    };
    private WeatherPrefsSnapshot() { }
    public static String[][] specs() {
        String[][] result = new String[KEYS.length][];
        for (int i = 0; i < result.length; i++) result[i] = KEYS[i].clone();
        return result;
    }
    public static Cursor cursor(SharedPreferences prefs) {
        if (prefs == null) return null;
        try {
            Map<String, ?> values = new HashMap<>(prefs.getAll());
            String[] columns = new String[KEYS.length + 1];
            columns[0] = "schema";
            for (int i = 0; i < KEYS.length; i++) columns[i + 1] = KEYS[i][1];
            MatrixCursor result = new MatrixCursor(columns);
            MatrixCursor.RowBuilder row = result.newRow().add(SCHEMA);
            for (String[] spec : KEYS) {
                Object value = values.get("prefs_key_" + spec[1]);
                if (value == null) { row.add(null); continue; }
                switch (spec[0]) {
                    case "boolean" -> {
                        if (!(value instanceof Boolean flag)) return null;
                        row.add(flag ? 1 : 0);
                    }
                    case "string" -> {
                        if (!(value instanceof String text)) return null;
                        int raw = Integer.parseInt(text);
                        if (spec[1].equals("weather_device_level") ? raw < 0 || raw > 3
                            : raw != 0 && raw != 2 && raw != 3) return null;
                        row.add(raw);
                    }
                    case "text" -> {
                        if (!(value instanceof String)) return null;
                        row.add(value);
                    }
                    case "long" -> {
                        if (!(value instanceof Long)) return null;
                        row.add(value);
                    }
                    default -> { return null; }
                }
            }
            return result;
        } catch (RuntimeException unavailable) { return null; }
    }
}
