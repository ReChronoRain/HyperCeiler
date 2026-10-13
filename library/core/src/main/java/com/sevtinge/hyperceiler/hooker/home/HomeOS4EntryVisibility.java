package com.sevtinge.hyperceiler.hooker.home;

import com.sevtinge.hyperceiler.core.R;

/** Keep the search index consistent with the OS4-only runtime preference replacements. */
public final class HomeOS4EntryVisibility {
    private HomeOS4EntryVisibility() {}

    public static boolean isHidden(int xml, String key, boolean xmlHidden, boolean os4) {
        if (!os4 || key == null) return xmlHidden;
        if (xml == R.xml.home_title_new) {
            if ("prefs_key_home_title_icon_size_enable".equals(key)
                || "prefs_key_home_title_icon_size".equals(key)) return true;
            if ("prefs_key_home_layout_icon_scale_enable".equals(key)
                || "prefs_key_home_layout_icon_scale".equals(key)) return false;
        } else if (xml == R.xml.home_layout) {
            if ("prefs_key_home_layout_icon_scale_enable".equals(key)
                || "prefs_key_home_layout_icon_scale".equals(key)
                || "prefs_key_home_layout_recents_hide_clear".equals(key)
                || "prefs_key_home_layout_recents_no_clear".equals(key)) return true;
        } else if (xml == R.xml.home_recent) {
            if ("prefs_key_home_recent_hide_clean_up".equals(key)
                || "prefs_key_always_show_clean_up".equals(key)) return true;
            if ("prefs_key_home_recent_clear_action_os4".equals(key)) return false;
        }
        return xmlHidden;
    }
}
