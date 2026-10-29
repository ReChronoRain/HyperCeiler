/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Map;

/** One authoritative physical-file revision; a null cell is a known missing key. */
public final class HomeLayoutPrefsSnapshot {
    public static final int SCHEMA = 4;
    public static final String RECENT_ICON_KEY = "home_recent_remove_icon";
    public static final String RECENT_TEXT_KEY = "home_recent_hide_text";
    public static final String RECENT_HEADER_VERSION = "hc_recent_header_version";
    public static final String RECENT_HEADER_FLAGS = "hc_recent_header_flags";
    public static final String DISMISS_KEY = "home_recent_modify_animation";
    public static final String DISMISS_VERSION = "hc_recent_dismiss_version";
    public static final String INFINITE_KEY = "home_other_infinite_scroll";
    public static final String INFINITE_VERSION = "hc_infinite_scroll_version";
    // Optional cursor extras preserve schema 4's exact columns for older clients.
    public static final String RECENTS_BAR_KEY = "home_recent_hide_status_bar_in_task_view";
    public static final String DOCK_HORIZONTAL_KEY = "home_layout_hotseats_margin_horizontal";
    public static final String DOCK_HORIZONTAL_VERSION = "hc_dock_horizontal_version";
    public static final String RECENTS_BAR_VERSION = "hc_recents_bar_version";
    public static final String RECENT_CARDS_KEY = "home_recent_hide_card";
    public static final String RECENT_CARDS_VERSION = "hc_recent_cards_version";
    private static final String[][] KEYS = {
        {"boolean", "home_drawer_hide_search"},
        {"integer", "task_view_corners"},
        {"integer", "task_view_header_height"},
        {"integer", "task_view_header_horizontal_offset"},
        {"integer", "home_recent_animation_speed"},
        {"integer", "home_recent_task_view_height"},
        {"integer", "task_view_horizontal1"},
        {"integer", "task_view_horizontal2"},
        {"integer", "home_recent_vertical_task_view_card_size"},
        {"integer", "home_recent_text_size"},
        {"integer", "home_recent_text_color"},
        {"integer", "home_recent_freeform_background_color"},

        {"boolean", "home_gesture_enable"},
        {"boolean", "home_navigation_quick_back"},
        {"integer", "home_gesture_double_tap_action"},
        {"integer", "home_gesture_up_swipe_action"},
        {"integer", "home_gesture_down_swipe_action"},
        {"integer", "home_gesture_up_swipe2_action"},
        {"integer", "home_gesture_down_swipe2_action"},
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
        {"boolean", "home_dock_icon_title"},
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
        {"boolean", "home_layout_small_folder_scale_enable"},
        {"integer", "home_layout_small_folder_scale"},
        {"boolean", "home_layout_big_folder_scale_enable"},
        {"integer", "home_layout_big_folder_scale"},
        {"boolean", "home_layout_widget_scale_enable"},
        {"integer", "home_layout_widget_scale"},

        {"boolean", "home_layout_recents_hide_clear"},
        {"boolean", "home_layout_recents_no_clear"},
        {"boolean", "home_animation_open_rate_enable"},
        {"integer", "home_animation_open_rate"},
        {"boolean", "home_animation_recents_enable"},
        {"integer", "home_animation_recents_rate"},
        {"boolean", "allow_hook"},
        {"boolean", "framework_api_allow_hook"},
        {"text", "framework_check_reason"},
        {"text", "framework_check_name"},
        {"text", "framework_check_version"},
        {"long", "framework_check_version_code"},
        {"boolean", "home_other_high_models"},
        {"boolean", "home_other_disable_prestart"},
        {"boolean", "home_dock_bg_custom_enable"},
        {"string", "home_dock_add_blur"},
        {"boolean", "home_title_hide_report_text"},
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
            Object hideBar = values.get("prefs_key_" + RECENTS_BAR_KEY);
            if (hideBar != null && !(hideBar instanceof Boolean)) return null;
            Bundle extras = new Bundle();
            Object dismiss = values.get("prefs_key_" + DISMISS_KEY);
            if (dismiss == null || dismiss instanceof Boolean) {
                extras.putInt(DISMISS_VERSION, 1);
                extras.putInt(DISMISS_KEY, Boolean.TRUE.equals(dismiss) ? 1 : 0);
            }
            extras.putInt(RECENTS_BAR_VERSION, 1);
            extras.putInt(RECENTS_BAR_KEY, Boolean.TRUE.equals(hideBar) ? 1 : 0);
            Object horizontal = values.get("prefs_key_" + DOCK_HORIZONTAL_KEY);
            Object horizontalEnabled = values.get("prefs_key_" + DOCK_HORIZONTAL_KEY + "_enable");
            if ((horizontal == null || horizontal instanceof Integer)
                && (horizontalEnabled == null || horizontalEnabled instanceof Boolean)) {
                int margin = horizontal instanceof Integer number ? number : 20;
                if (margin >= 0 && margin <= 100) {
                    extras.putInt(DOCK_HORIZONTAL_VERSION, 1);
                    extras.putInt(DOCK_HORIZONTAL_KEY, margin);
                    extras.putInt(DOCK_HORIZONTAL_KEY + "_enable", Boolean.TRUE.equals(horizontalEnabled) ? 1 : 0);
                }
            }
            Object infinite = values.get("prefs_key_" + INFINITE_KEY);
            if (infinite != null && !(infinite instanceof Boolean)) return null;
            extras.putInt(INFINITE_VERSION, 1);
            extras.putInt(INFINITE_KEY, Boolean.TRUE.equals(infinite) ? 1 : 0);
            // Optional extension, from the SAME getAll revision. A malformed
            // app selection must not invalidate unrelated launcher settings.
            Object hiddenCards = values.get("prefs_key_" + RECENT_CARDS_KEY);
            if (hiddenCards == null || hiddenCards instanceof java.util.Set<?>) {
                try {
                    String packet = encodeTitleRecords(hiddenCards == null ? java.util.Set.of()
                        : (java.util.Set<?>) hiddenCards);
                    extras.putString(RECENT_CARDS_KEY, packet);
                    extras.putInt(RECENT_CARDS_VERSION, 1);
                } catch (IllegalArgumentException invalid) { /* Extension unavailable, stock cards retained. */ }
            }
            Object hideIcon = values.get("prefs_key_" + RECENT_ICON_KEY);
            Object hideText = values.get("prefs_key_" + RECENT_TEXT_KEY);
            if ((hideIcon == null || hideIcon instanceof Boolean)
                && (hideText == null || hideText instanceof Boolean)) {
                extras.putInt(RECENT_HEADER_VERSION, 1);
                extras.putInt(RECENT_HEADER_FLAGS, (Boolean.TRUE.equals(hideIcon) ? 1 : 0)
                    | (Boolean.TRUE.equals(hideText) ? 2 : 0));
            }
            result.setExtras(extras);
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
