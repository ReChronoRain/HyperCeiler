/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

/** Internal preference names. The UI remains hidden until every required port group is ready. */
public final class StatusBarFollowPreferences {
    public static final String GROUP = "module";
    public static final String KEY_RESTORE_STATUS_BAR_FOLLOW = "system_ui_status_bar_follow_enable";
    public static final boolean DEFAULT_RESTORE_STATUS_BAR_FOLLOW = false;
    private StatusBarFollowPreferences() { }
}
