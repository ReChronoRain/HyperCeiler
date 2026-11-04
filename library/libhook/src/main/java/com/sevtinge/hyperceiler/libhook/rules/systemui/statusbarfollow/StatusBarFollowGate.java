/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

import android.os.Build;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;

import java.util.Set;

/** Default-off gate. Uncertain preferences, safe mode, or non-target versions keep stock. */
public final class StatusBarFollowGate {
    private StatusBarFollowGate() { }

    public static boolean enabled() {
        if (!com.sevtinge.hyperceiler.common.BuildConfig.STATUS_BAR_FOLLOW_AVAILABLE) return false;
        try {
            if (Build.VERSION.SDK_INT < 37) return false;
            if (!com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isHyperOSVersion(4f)) return false;
            if (PrefsBridge.getBoolean("home_safe_mode_enable", false)
                || PrefsBridge.getBoolean("system_ui_safe_mode_enable", false)) return false;
            Set<String> aliases = CrashScope.getCrashingAliases();
            if (aliases.contains("home") || aliases.contains("systemui")) return false;
            return PrefsBridge.getBoolean(
                StatusBarFollowPreferences.KEY_RESTORE_STATUS_BAR_FOLLOW,
                StatusBarFollowPreferences.DEFAULT_RESTORE_STATUS_BAR_FOLLOW);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
