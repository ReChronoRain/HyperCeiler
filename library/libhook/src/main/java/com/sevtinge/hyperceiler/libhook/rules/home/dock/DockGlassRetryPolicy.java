/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

/** Bounded per-ticket recovery; never driven by the window's frame rate. */
public final class DockGlassRetryPolicy {
    public static final int BACKGROUND_CHECKS = 20;
    public static final long BACKGROUND_CHECK_MS = 500L;
    /** Saturating check count: subsequent checks all use the idle cadence. */
    public static final int BACKGROUND_IDLE_CHECK = BACKGROUND_CHECKS + 10;
    private static final long[] DELAYS_MS = {2000, 4000, 8000, 16000, 30000};

    /** Faster than the compatibility ladder: a package swap is expected to heal in seconds. */
    private static final long[] DEPENDENCY_DELAYS_MS = {1000, 2000, 4000, 8000, 16000, 30000};

    private DockGlassRetryPolicy() {}

    /**
     * Delay after an unready status, or -1 when a missing/inactive producer needs recovery.
     * A live attached producer can wait for background damage without losing its generation
     * or consuming compatibility attempts. Readiness checks stop when ready or hidden.
     */
    public static long delayAfterBackgroundCheck(int checks, boolean attached, boolean producerActive) {
        if (checks < BACKGROUND_CHECKS) return BACKGROUND_CHECK_MS;
        if (!attached || !producerActive) return -1L;
        return checks < BACKGROUND_IDLE_CHECK ? 3000L : 15000L;
    }

    public static long delayAfterFailure(int failedAttempts) {
        if (failedAttempts < 1 || failedAttempts > DELAYS_MS.length) return -1;
        return DELAYS_MS[failedAttempts - 1];
    }

    /**
     * A host that has rendered native glass successfully is known to be compatible. Keep
     * recovering it at the capped cadence after transient process death instead of making a
     * launcher restart the only way to reset the retry budget.
     */
    public static long delayAfterFailure(int failedAttempts, boolean previouslyReady) {
        if (failedAttempts < 1) return -1;
        if (failedAttempts <= DELAYS_MS.length) return DELAYS_MS[failedAttempts - 1];
        return previouslyReady ? DELAYS_MS[DELAYS_MS.length - 1] : -1;
    }

    /**
     * Recreate a lost live renderer immediately. Failed initialization still uses the ordinary
     * bounded backoff, preventing a provider crash loop.
     */
    public static long delayAfterRuntimeFailure(
            int consecutiveFailures, boolean wasReady, boolean previouslyReady) {
        if (wasReady) return 0;
        return delayAfterFailure(consecutiveFailures, previouslyReady);
    }

    /**
     * {@code PackageManager} briefly could not resolve the HyperCeiler package, typically
     * because it was being replaced by an in-place upgrade.
     *
     * <p>This is transient and unrelated to framework compatibility, so it never exhausts:
     * giving up would strand a ticket that had merely raced an install and make a launcher
     * restart the only way to bring native glass back. The cadence is capped at 30s instead,
     * which keeps a stuck outage from turning into a hot retry loop.
     */
    public static long delayAfterDependencyUnavailable(int unavailableFailures) {
        if (unavailableFailures < 1) return DEPENDENCY_DELAYS_MS[0];
        if (unavailableFailures <= DEPENDENCY_DELAYS_MS.length) {
            return DEPENDENCY_DELAYS_MS[unavailableFailures - 1];
        }
        return DEPENDENCY_DELAYS_MS[DEPENDENCY_DELAYS_MS.length - 1];
    }
}
