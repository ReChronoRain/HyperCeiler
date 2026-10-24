/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import java.util.Map;

/** Separate schema: adding gesture strings must not invalidate the native layout protocol. */
public final class HomeGesturePrefsSnapshot {
    public static final String[] ACTIONS = {"home_gesture_double_tap", "home_gesture_up_swipe",
        "home_gesture_down_swipe", "home_gesture_up_swipe2", "home_gesture_down_swipe2",
        "home_gesture_shake", "home_navigation_assist_left_slide", "home_navigation_assist_right_slide",
        "home_gesture_line_long_press", "home_gesture_line_double_click"};
    // Events 6/7 retain the existing Previous wire contract. Native mask remains exactly five bits.
    public static final int NATIVE_ACTION_COUNT = 5;
    private HomeGesturePrefsSnapshot() {}
    public static Cursor cursor(SharedPreferences prefs) {
        if (prefs == null) return null;
        Map<String, ?> values = prefs.getAll();
        java.util.List<String> columns = new java.util.ArrayList<>();
        columns.add("schema"); columns.add("home_gesture_enable"); columns.add("home_navigation_quick_back");
        for (String key : ACTIONS) {
            columns.add(key + "_action"); columns.add(key + "_app");
            columns.add(key + "_activity"); columns.add(key + "_shortcut_intent");
        }
        MatrixCursor cursor = new MatrixCursor(columns.toArray(new String[0]));
        MatrixCursor.RowBuilder row = cursor.newRow().add(1);
        for (int i = 1; i < columns.size(); i++) {
            String key = columns.get(i);
            Object value = values.get("prefs_key_" + key);
            if (value == null) row.add(null);
            else if (value instanceof Boolean flag) row.add(flag ? 1 : 0);
            else if (value instanceof Integer || value instanceof String) row.add(value);
            else { cursor.close(); return null; }
        }
        return cursor;
    }
}
