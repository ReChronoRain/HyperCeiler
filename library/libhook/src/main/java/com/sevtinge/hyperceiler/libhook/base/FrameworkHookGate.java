/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.base;

import java.util.function.Supplier;

/** The same stored-framework decision for Java entry and the authenticated native endpoint. */
public final class FrameworkHookGate {
    private FrameworkHookGate() { }

    public record Fingerprint(String name, String version, long versionCode) {
        public Fingerprint {
            name = normalize(name);
            version = normalize(version);
        }
    }

    public static boolean isAllowed(boolean storedAllow, String blockedReason,
                                    Supplier<Fingerprint> running, Supplier<Fingerprint> checked) {
        if (storedAllow) return true;
        // Keep the entry's existing distinction between a missing reason and a recorded reason.
        if (blockedReason == null || blockedReason.isEmpty()) return false;
        final Fingerprint current;
        try {
            current = running.get();
        } catch (Throwable unavailable) {
            return false;
        }
        if (current == null) return false;
        final Fingerprint prior = checked.get();
        return prior != null && !current.equals(prior);
    }

    private static String normalize(String value) {
        if (value == null) return null;
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
