/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import java.util.HashMap;
import java.util.Map;

/** One authoritative physical-file revision; a null cell is a known missing key. */
public final class HomeLayoutPrefsSnapshot {
    public static final int SCHEMA = 1;
    private static final String[][] KEYS = {
        {"boolean", "home_layout_unlock_grids_new"},
        {"integer", "home_layout_unlock_grids_cell_x"},
        {"integer", "home_layout_unlock_grids_cell_y"},
        {"boolean", "home_layout_hotseats_margin_bottom_enable"},
        {"integer", "home_layout_hotseats_margin_bottom"},
        {"boolean", "home_folder_vertical_spacing_enable"},
        {"integer", "home_folder_vertical_spacing"},
        {"boolean", "home_layout_workspace_padding_top_enable"},
        {"integer", "home_layout_workspace_padding_top"},
        {"boolean", "home_layout_workspace_padding_bottom_enable"},
        {"integer", "home_layout_workspace_padding_bottom"},
        {"boolean", "home_layout_workspace_padding_horizontal_enable"},
        {"integer", "home_layout_workspace_padding_horizontal"},
        {"boolean", "home_layout_indicator_margin_bottom_enable"},
        {"integer", "home_layout_indicator_margin_bottom"},
        {"string", "home_other_seek_points"},
        {"boolean", "home_dock_unlock_hotseat"},
        {"boolean", "home_layout_searchbar_margin_bottom_enable"},
        {"integer", "home_layout_searchbar_margin_bottom"},
        {"boolean", "home_layout_searchbar_width_enable"},
        {"integer", "home_layout_searchbar_width"},
        {"integer", "home_folder_columns"},
        {"boolean", "home_layout_pad_grid_enable"},
        {"integer", "home_layout_pad_major"},
        {"integer", "home_layout_pad_minor"},
        {"boolean", "home_layout_fold_grid_enable"},
        {"integer", "home_layout_fold_major"},
        {"integer", "home_layout_fold_minor"},
        {"boolean", "home_layout_icon_scale_enable"},
        {"integer", "home_layout_icon_scale"},
        {"boolean", "home_layout_recents_hide_clear"},
        {"boolean", "home_layout_recents_no_clear"},
        {"boolean", "home_animation_open_rate_enable"},
        {"integer", "home_animation_open_rate"},
        {"boolean", "home_animation_recents_enable"},
        {"integer", "home_animation_recents_rate"},
    };
    private HomeLayoutPrefsSnapshot() {}
    public static String[][] specs() {
        String[][] result = new String[KEYS.length][];
        for (int i = 0; i < result.length; i++) result[i] = KEYS[i].clone();
        return result;
    }
    public static Cursor cursor(SharedPreferences prefs) {
        if (prefs == null) return null;
        try {
            // getAll() snapshots SharedPreferences under its own lock. Do not interleave
            // contains()/getInt() Binder calls with UI commits or package replacement.
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
                    case "integer" -> {
                        if (!(value instanceof Integer)) return null;
                        row.add(value);
                    }
                    case "string" -> {
                        if (!(value instanceof String text)) return null;
                        try { row.add(Integer.parseInt(text)); }
                        catch (NumberFormatException invalid) { return null; }
                    }
                    default -> { return null; }
                }
            }
            return result;
        } catch (RuntimeException unavailable) { return null; }
    }
}
