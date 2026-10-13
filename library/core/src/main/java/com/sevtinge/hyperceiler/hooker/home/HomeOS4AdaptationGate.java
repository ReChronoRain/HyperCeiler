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
        "prefs_key_home_title_title_icontitlecustomization_onoff",
        "prefs_key_home_title_title_icontitlecustomization",
        "prefs_key_home_title_title_new_install", "prefs_key_home_title_title_color",
        "prefs_key_home_title_font_size", "prefs_key_home_drawer_title_font_size");
    // Keep the OS4 clear-button implementation and the framework-backed home-mode setting.
    private static final Set<String> ADAPTED_RECENT_KEYS = Set.of("prefs_key_home_recent_clear_action_os4");
    private static final Set<String> ADAPTED_OTHER_KEYS = Set.of(
        "prefs_key_home_folder_auto_close", "prefs_key_home_other_home_mode", "prefs_key_home_widget_allow_moved_to_minus_one_screen");

    private HomeOS4AdaptationGate() {}

    public static void apply(PreferenceGroup screen, int xml) {
        if (!isHyperOSVersion(4f)) return;
        if (xml == R.xml.home_dock) {
            Preference title = screen.findPreference("prefs_key_home_dock_icon_title");
            if (title != null) title.setEnabled(false);
        } else if (xml == R.xml.home_title_new) {
            disableLeaves(screen, ADAPTED_TITLE_KEYS);
        } else if (xml == R.xml.home_recent) {
            disableLeaves(screen, ADAPTED_RECENT_KEYS);
        } else if (xml == R.xml.home_other_new) {
            disableLeaves(screen, ADAPTED_OTHER_KEYS);
        } else if (xml == R.xml.home_gesture) {
            disableLeaves(screen, Set.of("prefs_key_home_navigation_back_area_height",
                "prefs_key_home_navigation_back_area_width"));
        } else if (xml == R.xml.home_drawer) {
            disableLeaves(screen, Set.of());
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
