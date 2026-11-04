/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import java.util.List;
import java.util.Set;

/** A16 ShadeStatusBarTokenInteractor policy; independent of gesture alpha policy. */
final class XiaomiShadeBarSelection {
    static final int HOME = 0;
    static final int APP = 1;
    private static final Set<String> TRANSPARENT_EXCEPTIONS = Set.of(
            "com.miui.gallery.activity.InternalPhotoPageActivity",
            "com.miui.gallery.search.SearchActivity",
            "com.android.phone.settings.MobileNetworkSettings",
            "com.miui.auth.PrivacyPasswordActivity");
    private static final String HOME_EXCEPTION = "com.android.phone.settings.MobileNetworkSettings";

    static final class Root {
        final int displayId;
        final int windowingMode;
        final int activityType;
        final boolean transparent;
        final String topClass;

        Root(int displayId, int windowingMode, int activityType, boolean transparent, String topClass) {
            this.displayId = displayId;
            this.windowingMode = windowingMode;
            this.activityType = activityType;
            this.transparent = transparent;
            this.topClass = topClass;
        }
    }

    private Boolean home;

    /** Native appTransitionEvent is a display ID, filtered to zero, not a gesture state. */
    boolean onAppTransition(int displayId, Root focused, List<Root> orderedRootsOnDisplay) {
        if (displayId != 0) return false;
        Root selected = focused != null && focused.displayId == 0 && eligible(focused) ? focused : null;
        if (selected == null && orderedRootsOnDisplay != null) {
            for (Root candidate : orderedRootsOnDisplay) {
                if (candidate == null || candidate.displayId != displayId) return false;
                if (eligible(candidate)) {
                    selected = candidate;
                    break;
                }
            }
        }
        // mapNotNull: an absent result must not manufacture a Home/App change.
        if (selected == null) return false;
        boolean next = selected.activityType == 2 && !HOME_EXCEPTION.equals(selected.topClass);
        boolean changed = home == null || home != next;
        home = next;
        return changed;
    }

    int current(boolean flipFolded, boolean appTokenAvailable) {
        // stateIn begins with DEFAULT_STATUS_BAR_TOKEN until task flow emits.
        return home == null || home || flipFolded || !appTokenAvailable ? HOME : APP;
    }

    void invalidate() { home = null; }

    static boolean eligible(Root root) {
        // Verified against A16 mapNotNull emit smali, including :cond_79 loop.
        return root.windowingMode == 1 && (!root.transparent
                || root.topClass != null && TRANSPARENT_EXCEPTIONS.contains(root.topClass));
    }
}
