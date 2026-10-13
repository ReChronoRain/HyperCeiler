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
        {"integer", "home_navigation_back_area_height"},
        {"integer", "home_navigation_back_area_width"},
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
        {"boolean", "home_widget_allow_moved_to_minus_one_screen"},
        {"boolean", "home_folder_auto_close"},
        {"integer", "home_title_font_size"},
        {"integer", "home_drawer_title_font_size"},
        {"integer", "home_title_title_color"},
        {"boolean", "home_title_title_new_install"},
        {"boolean", "home_title_title_icontitlecustomization_onoff"},
        {"stringset", "home_title_title_icontitlecustomization"},
        {"boolean", "home_layout_searchbar_margin_bottom_enable"},
        {"integer", "home_layout_searchbar_margin_bottom"},
        {"boolean", "home_layout_searchbar_width_enable"},
        {"integer", "home_layout_searchbar_width"},
        {"integer", "home_folder_columns"},
        {"string", "home_folder_title_pos"},
        {"boolean", "home_folder_width"},
        {"boolean", "home_folder_horizontal_padding_enable"},
        {"integer", "home_folder_horizontal_padding"},
        {"integer", "home_folder_horizontal_padding_pad_h"},
        {"integer", "home_folder_horizontal_padding_pad_v"},
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
                    case "stringset" -> {
                        if (!(value instanceof java.util.Set<?> records)) return null;
                        row.add(encodeTitleRecords(records));
                    }
                    default -> { return null; }
                }
            }
            return result;
        } catch (RuntimeException unavailable) { return null; }
    }

    /** Length framing preserves Unicode, newlines and the existing UI's record format. */
    public static String encodeTitleRecords(java.util.Set<?> records) {
        if (records.size() > 1024) throw new IllegalArgumentException("too many title records");
        final java.util.TreeSet<String> sorted = new java.util.TreeSet<>();
        for (Object value : records) {
            if (!(value instanceof String text) || text.length() > 1024)
                throw new IllegalArgumentException("invalid title record");
            sorted.add(text);
        }
        StringBuilder result = new StringBuilder().append(sorted.size()).append(':');
        for (String text : sorted) result.append(text.length()).append(':').append(text);
        if (result.length() > 131072) throw new IllegalArgumentException("title packet too large");
        return result.toString();
    }

    public static java.util.Set<String> decodeTitleRecords(String packet) {
        if (packet == null || packet.length() > 131072) throw new IllegalArgumentException("title packet");
        int[] at = {0};
        int count = readLength(packet, at, 1024);
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (int index = 0; index < count; ++index) {
            int length = readLength(packet, at, 1024);
            if (length > packet.length() - at[0]) throw new IllegalArgumentException("short title record");
            result.add(packet.substring(at[0], at[0] + length)); at[0] += length;
        }
        if (at[0] != packet.length()) throw new IllegalArgumentException("trailing title packet");
        return result;
    }

    private static int readLength(String text, int[] at, int max) {
        int value = 0, start = at[0];
        while (at[0] < text.length() && text.charAt(at[0]) != ':') {
            char digit = text.charAt(at[0]++);
            if (digit < '0' || digit > '9') throw new IllegalArgumentException("title length");
            value = value * 10 + digit - '0';
            if (value > max) throw new IllegalArgumentException("title length bound");
        }
        if (at[0] == start || at[0] == text.length()) throw new IllegalArgumentException("title length missing");
        ++at[0]; return value;
    }

    private static boolean validTitle(String label) {
        for (int i = 0; i < label.length(); ++i) {
            char unit = label.charAt(i);
            if (unit == 0) return false;
            if (Character.isHighSurrogate(unit)) {
                if (++i >= label.length() || !Character.isLowSurrogate(label.charAt(i))) return false;
            } else if (Character.isLowSurrogate(unit)) return false;
        }
        return true;
    }

    /** Existing AppEditManager records are package + ฿ + title + ฿ + random suffix. */
    public static String[][] customTitles(String packet) {
        if (packet == null) return new String[0][];
        java.util.Map<String, String> names = new java.util.TreeMap<>();
        int units = 0;
        for (String record : decodeTitleRecords(packet)) {
            int first = record.indexOf('฿'), last = record.lastIndexOf('฿');
            if (first < 1 || last <= first) continue;
            String pkg = record.substring(0, first), label = record.substring(first + 1, last);
            if (pkg.length() > 255 || !pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")
                || label.isEmpty() || label.length() > 512 || !validTitle(label)) continue;
            if (names.containsKey(pkg)) continue;
            if (units + pkg.length() + label.length() > 32768) break;
            units += pkg.length() + label.length(); names.put(pkg, label);
        }
        String[][] result = new String[names.size()][2]; int index = 0;
        for (var entry : names.entrySet()) result[index++] = new String[]{entry.getKey(), entry.getValue()};
        return result;
    }
}
