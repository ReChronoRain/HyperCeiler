/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.log;

/** Canonical names for new output and records imported from earlier module builds. */
public final class HomeLayoutEngineLogNames {
    public static final String TAG = "HyperCeiler.HomeLayoutEngine";
    // Read compatibility only: older processes may still run until the next restart.
    private static final String LEGACY_TAG = "HomeTweaks";

    private HomeLayoutEngineLogNames() {}

    static boolean isLegacyTag(String tag) {
        return LEGACY_TAG.equals(tag) || "HyperCeiler.HomePatch".equals(tag) || "HomePatch".equals(tag);
    }

    public static String tag(String tag) {
        return isLegacyTag(tag) ? TAG : text(tag);
    }

    public static String text(String text) {
        if (text == null) return null;
        return text.replace(LEGACY_TAG, "HomeLayoutEngine")
            .replace("HomePatch", "HomeLayoutEngine")
            .replace("home_patches", "home_layout_engine")
            .replace("home_patch", "home_layout_engine")
            .replace("hometweaks", "home_layout_engine")
            .replace("home_tweaks", "home_layout_engine")
            .replace("home-tweaks", "home-layout-engine");
    }
}
