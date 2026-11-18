// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

/** Identifies a completed home intent before the temporary launcher surface can play. */
final class XiaoheiziEntryGate {
    // Recents dismissal can send a home animation immediately before selecting an app.
    static final long SETTLE_MS = 110;
    private static final long MAX_AGE_MS = 1_500;

    enum Decision { IGNORE, ARM, CANCEL }
    private long homeAt = -1;

    Decision onCommand(String command, String action, double scale, long now) {
        if (Boolean.TRUE.equals(DockRecentsMotion.overviewTarget(command, action, scale))
                || DockRecentsMotion.leavingHomeTarget(command, action, scale)) {
            reset();
            return Decision.CANCEL;
        }
        // The stock recents prelude also sends setTo with the home scale.
        if (!"startAnim".equals(action) || !DockRecentsMotion.homeTarget(command, action, scale)) {
            return Decision.IGNORE;
        }
        homeAt = now;
        return Decision.ARM;
    }

    boolean ready(long now) {
        return homeAt >= 0 && now - homeAt >= SETTLE_MS && now - homeAt < MAX_AGE_MS;
    }

    void reset() {
        homeAt = -1;
    }
}
