/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeRecentCardsFilter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A selection from the same physical getAll revision as layout and feedback. */
public final class HomeRecentCardsPrefs {
    private HomeRecentCardsPrefs() {}

    /** Null is unavailable, an empty set is an authoritative reset. */
    public static Set<String> selection(Map<String, ?> values) {
        Object value = values.get("prefs_key_" + HomeLayoutPrefsSnapshot.RECENT_CARDS_KEY);
        if (value != null && !(value instanceof Set<?>)) return null;
        var names = new HashSet<String>();
        if (value instanceof Set<?> records) {
            for (Object record : records) {
                if (!(record instanceof String name)) return null;
                names.add(name);
            }
        }
        final Set<String> packages;
        try {
            packages = HomeRecentCardsFilter.packages(names);
            // Preserve the existing Provider extension's framing budget too.
            HomeLayoutPrefsSnapshot.encodeTitleRecords(packages);
        }
        catch (IllegalArgumentException invalid) { return null; }
        return Boolean.FALSE.equals(values.get("prefs_key_allow_hook"))
            || Boolean.FALSE.equals(values.get("prefs_key_framework_api_allow_hook"))
            ? Set.of() : packages;
    }
}
