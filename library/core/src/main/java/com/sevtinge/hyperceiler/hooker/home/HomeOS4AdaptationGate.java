package com.sevtinge.hyperceiler.hooker.home;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isHyperOSVersion;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.sevtinge.hyperceiler.core.R;

import java.util.Set;

/** UI-only adaptation status: never reset preferences or replace their original descriptions. */
public final class HomeOS4AdaptationGate {
    private static final Set<String> ADAPTED_TITLE_KEYS = Set.of(
        "prefs_key_home_layout_icon_scale_enable", "prefs_key_home_layout_icon_scale",
        "prefs_key_home_layout_small_folder_scale_enable",
        "prefs_key_home_layout_small_folder_scale",
        "prefs_key_home_layout_big_folder_scale_enable",
        "prefs_key_home_layout_big_folder_scale",
        "prefs_key_home_layout_widget_scale_enable",
        "prefs_key_home_layout_widget_scale",

        "prefs_key_home_title_title_icontitlecustomization_onoff",
        "prefs_key_home_title_title_icontitlecustomization",
        "prefs_key_home_title_title_new_install", "prefs_key_home_title_title_color",
        "prefs_key_home_title_font_size", "prefs_key_home_drawer_title_font_size");
    // Keep the OS4 clear-button implementation and the framework-backed home-mode setting.
    private static final Set<String> ADAPTED_RECENT_KEYS = Set.of(
        "prefs_key_home_recent_clear_action_os4", "prefs_key_task_view_corners",
        "prefs_key_task_view_header_height", "prefs_key_task_view_header_horizontal_offset",
        "prefs_key_home_recent_animation_speed", "prefs_key_home_recent_task_view_height",
        "prefs_key_task_view_horizontal1", "prefs_key_task_view_horizontal2",
        "prefs_key_home_recent_vertical_task_view_card_size", "prefs_key_home_recent_text_size",
        "prefs_key_home_recent_text_color", "prefs_key_home_recent_freeform_background_color");
    private static final Set<String> ADAPTED_OTHER_KEYS = Set.of(
        "prefs_key_home_folder_auto_close", "prefs_key_home_other_home_mode", "prefs_key_home_widget_allow_moved_to_minus_one_screen",
        "prefs_key_home_other_disable_prestart", "prefs_key_home_title_hide_report_text");

    // Keep the UI in sync with the verified OS4 native workspace/quick-switch paths.
    private static final Set<String> ADAPTED_GESTURE_KEYS = Set.of(
        "prefs_key_home_gesture_enable", "prefs_key_home_navigation_quick_back",
        "prefs_key_home_gesture_double_tap", "prefs_key_home_gesture_shake",
        "prefs_key_home_navigation_assist_left_slide", "prefs_key_home_navigation_assist_right_slide",
        "prefs_key_home_gesture_line_long_press", "prefs_key_home_gesture_line_double_click",
        "prefs_key_home_gesture_up_swipe", "prefs_key_home_gesture_down_swipe",
        "prefs_key_home_gesture_up_swipe2", "prefs_key_home_gesture_down_swipe2",
        "prefs_key_home_navigation_back_area_height", "prefs_key_home_navigation_back_area_width");

    private HomeOS4AdaptationGate() {}

    public static void apply(PreferenceGroup screen, int xml) {
        if (!isHyperOSVersion(4f)) return;
        if (xml == R.xml.home_title_new) {
            disableLeaves(screen, ADAPTED_TITLE_KEYS);
        } else if (xml == R.xml.home_recent) {
            disableLeaves(screen, ADAPTED_RECENT_KEYS);
        } else if (xml == R.xml.home_other_new) {
            disableLeaves(screen, ADAPTED_OTHER_KEYS);
        } else if (xml == R.xml.home_gesture) {
            disableLeaves(screen, ADAPTED_GESTURE_KEYS);
        } else if (xml == R.xml.home_drawer) {
            disableLeaves(screen, Set.of("prefs_key_home_drawer_hide_search"));
        }
    }

    private static void disableLeaves(PreferenceGroup group, Set<String> adaptedKeys) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup child) {
                disableLeaves(child, adaptedKeys);
            } else {
                String key = preference.getKey();
                // Gate features, not widget classes: sliders, lists, colors and entries count too.
                // Leave recommendations/categories and adapted controls' dependency state alone.
                if (key != null && !key.isEmpty() && !adaptedKeys.contains(key)) {
                    preference.setEnabled(false);
                }
            }
        }
    }
}
