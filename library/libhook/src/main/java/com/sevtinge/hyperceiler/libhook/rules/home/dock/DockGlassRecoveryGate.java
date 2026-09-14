/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

/**
 * Lifecycle state machine for one glass ticket: generation counter, single-flight
 * recovery latch, bounded retry budget and stale-request rejection.
 *
 * <p>It is deliberately Android-free and owns bookkeeping only. The resource handles
 * (SurfacePackage, process lease, provider client, death recipient) stay in the ticket,
 * so the host test suite can drive every transition here without an Android runtime.
 *
 * <p>Generation/recovery bookkeeping runs on the serial IPC worker. Refresh and readiness
 * epochs share a lock because the launcher/WMS thread can pause or retire them immediately.
 */
public final class DockGlassRecoveryGate {

    /** Wall-clock minimum between two visibility-driven refresh probes. */
    private static final long REFRESH_DEDUPLICATION_MS = 250L;

    /** Returned by {@link #requestRefresh} when the request was deduplicated. */
    public static final int REFRESH_DEDUPLICATED = 0;

    /**
     * How many dependency outages are logged per burst. A package replacement is routine and
     * must not flood the system log while it happens, but the first entries have to make the
     * cause obvious to anyone reading logcat.
     */
    private static final int DEPENDENCY_DEFER_REPORTS = 3;

    /** How many stale-callback notices are logged per ticket. */
    private static final int STALE_CALLBACK_REPORTS = 4;

    /** What the caller should do after asking for a recovery. */
    public enum Outcome {
        /** Retired or already recovering: the caller must not schedule anything. */
        IGNORED,
        /** Retry immediately. */
        RETRY_NOW,
        /** Retry after {@link #getRetryDelayMs()}. */
        RETRY_LATER,
        /** The bounded budget is spent: keep the compositor fallback. */
        EXHAUSTED
    }

    private int attempts;
    private boolean recoveryPending;
    private boolean previouslyReady;
    private int consecutiveFailures;
    private int unavailableFailures;
    private int dependencyDeferReports;
    private int deduplicatedRecoveries;
    private int staleCallbackReports;
    private int readinessEpoch;
    private long retryDelayMs;

    private volatile boolean cancelled;
    private volatile boolean retired;

    private final Object refreshLock = new Object();
    private int refreshRequestEpoch;
    private long lastRefreshRequest;
    private boolean departureHeld;
    private boolean refreshAllowed = true;

    // --- generation -------------------------------------------------------------

    /**
     * Admit a create or recovery attempt and open a new generation.
     *
     * @return false when the ticket is retired or the client is closing; the caller must
     *         then return without touching any resource.
     */
    public boolean beginAttempt(boolean closed) {
        if (retired || cancelled || closed) return false;
        recoveryPending = false;
        attempts++;
        return true;
    }

    public int getAttempts() {
        return attempts;
    }

    /** True while {@code generation} is still the ticket's active, live generation. */
    public boolean isAttemptCurrent(int generation) {
        return !retired && !cancelled && generation == attempts;
    }

    /** Open a fresh readiness loop, invalidating every earlier one. */
    public int openReadinessEpoch() {
        synchronized (refreshLock) {
            return ++readinessEpoch;
        }
    }

    /** True while both the generation and its readiness loop are still live. */
    public boolean isReadinessCurrent(int generation, int epoch) {
        synchronized (refreshLock) {
            return !retired && !cancelled && refreshAllowed
                    && generation == attempts && epoch == readinessEpoch;
        }
    }

    /** Invalidate pending readiness loops without opening a new one. */
    public void invalidateReadiness() {
        synchronized (refreshLock) {
            readinessEpoch++;
        }
    }

    // --- recovery ---------------------------------------------------------------

    /**
     * Claim the single-flight recovery latch for a failed generation.
     *
     * <p>Repeated calls while a retry is pending are {@link Outcome#IGNORED}, which is what
     * keeps a burst of callbacks (death recipient, readiness loop, attachment probe) from
     * creating parallel retries for the same ticket.
     *
     * @param wasReady whether the retired generation had displayed native glass
     */
    public Outcome requestRecovery(boolean closed, boolean wasReady) {
        if (retired || cancelled || closed) return Outcome.IGNORED;
        if (recoveryPending) {
            // A burst of callbacks described the same failure; keep the one retry already
            // scheduled rather than queueing parallel ones.
            deduplicatedRecoveries++;
            return Outcome.IGNORED;
        }
        recoveryPending = true;
        if (!wasReady) consecutiveFailures++;
        final long delay = DockGlassRetryPolicy.delayAfterRuntimeFailure(
                consecutiveFailures, wasReady, previouslyReady);
        retryDelayMs = delay;
        if (delay < 0) return Outcome.EXHAUSTED;
        return delay == 0 ? Outcome.RETRY_NOW : Outcome.RETRY_LATER;
    }

    /** Recoveries collapsed into an already-pending retry, for the diagnostic log. */
    public int getDeduplicatedRecoveries() {
        return deduplicatedRecoveries;
    }

    /**
     * Note that a callback for an older generation or a cancelled readiness loop was dropped.
     *
     * @return true for the first few notices, so a caller polling every frame cannot spam
     */
    public boolean noteStaleCallback() {
        if (staleCallbackReports >= STALE_CALLBACK_REPORTS) return false;
        staleCallbackReports++;
        return true;
    }

    /**
     * Claim the latch for an outage that is expected to heal on its own, such as the
     * HyperCeiler package being replaced by an upgrade.
     *
     * <p>This deliberately does <em>not</em> consume the compatibility budget: a slow
     * package replacement must not permanently spend a ticket that had never rendered.
     * The cadence is capped instead, so a retry loop stays bounded.
     */
    public Outcome requestDependencyDefer(boolean closed) {
        if (retired || cancelled || closed || recoveryPending) return Outcome.IGNORED;
        recoveryPending = true;
        unavailableFailures++;
        retryDelayMs = DockGlassRetryPolicy.delayAfterDependencyUnavailable(unavailableFailures);
        return retryDelayMs == 0 ? Outcome.RETRY_NOW : Outcome.RETRY_LATER;
    }

    /** Delay for the last {@link Outcome#RETRY_LATER} decision, in milliseconds. */
    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    /**
     * Claim the right to log one dependency outage.
     *
     * @return true for the first few outages of a burst, then false so a slow package swap
     *         cannot spam the system log
     */
    public boolean shouldReportDependencyDefer() {
        if (dependencyDeferReports >= DEPENDENCY_DEFER_REPORTS) return false;
        dependencyDeferReports++;
        return true;
    }

    /**
     * A generation rendered successfully: the retry budget is healthy again and the
     * ticket is known to be compatible with this framework build.
     */
    public void markReady() {
        previouslyReady = true;
        consecutiveFailures = 0;
        unavailableFailures = 0;
        dependencyDeferReports = 0;
        staleCallbackReports = 0;
    }

    public boolean isPreviouslyReady() {
        return previouslyReady;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    // --- visibility probe (launcher/WMS thread and worker) -----------------------

    /**
     * Register a visibility-driven refresh request.
     *
     * @return the request epoch to validate later, or {@link #REFRESH_DEDUPLICATED} when a
     *         recent request already covers this return
     */
    public int requestRefresh(long nowUptimeMillis) {
        return requestRefresh(nowUptimeMillis, false);
    }

    /** Blocked sampling must neither consume the deduplication window nor cancel a pause. */
    public int requestRefresh(long nowUptimeMillis, boolean captureBlocked) {
        synchronized (refreshLock) {
            if (captureBlocked || departureHeld) return REFRESH_DEDUPLICATED;
            refreshAllowed = true;
            if (nowUptimeMillis - lastRefreshRequest < REFRESH_DEDUPLICATION_MS) {
                return REFRESH_DEDUPLICATED;
            }
            lastRefreshRequest = nowUptimeMillis;
            return ++refreshRequestEpoch;
        }
    }

    /**
     * A rotation return must always probe, even when a visibility resume already described the
     * same return within the deduplication window. The host recomputes its blur geometry inside
     * that probe, so swallowing it would leave the panel on the rotated sample.
     */
    public int forceRefresh(long nowUptimeMillis) {
        synchronized (refreshLock) {
            if (retired || cancelled) return REFRESH_DEDUPLICATED;
            refreshAllowed = true;
            lastRefreshRequest = nowUptimeMillis;
            return ++refreshRequestEpoch;
        }
    }

    /** Abandon only this queued probe, allowing the next portrait return to retry immediately. */
    public void cancelRefresh(int epoch) {
        synchronized (refreshLock) {
            if (epoch != refreshRequestEpoch) return;
            refreshRequestEpoch++;
            lastRefreshRequest = 0;
        }
    }

    /** True while the request behind {@code epoch} is still wanted. */
    public boolean isRefreshCurrent(int epoch) {
        synchronized (refreshLock) {
            return !retired && refreshAllowed && epoch == refreshRequestEpoch;
        }
    }

    /** Cancel delayed refresh/readiness work while the launcher parent is hidden. */
    public int pauseRefresh() {
        synchronized (refreshLock) {
            refreshAllowed = false;
            refreshRequestEpoch++;
            readinessEpoch++;
            lastRefreshRequest = 0;
            return refreshRequestEpoch;
        }
    }

    /** Preserve the home sample before the app-launch wallpaper zoom changes its region. */
    public int holdForDeparture() {
        synchronized (refreshLock) {
            if (retired || cancelled || departureHeld) return REFRESH_DEDUPLICATED;
            departureHeld = true;
            return pauseRefresh();
        }
    }

    /** A home/overview command or a real visibility return can end the departure hold. */
    public void releaseDepartureHold() {
        synchronized (refreshLock) {
            departureHeld = false;
        }
    }

    /** A delayed sampling pause must not freeze a host after a newer return to the launcher. */
    public boolean isPauseCurrent(int epoch) {
        synchronized (refreshLock) {
            return !retired && !cancelled && !refreshAllowed && epoch == refreshRequestEpoch;
        }
    }

    // --- retirement -------------------------------------------------------------

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Retire the ticket exactly once and invalidate every pending loop for its last generation.
     *
     * <p>The invalidation matters even though the caller also checks the retired flag: a
     * readiness callback that was already scheduled for this generation must not be able to
     * look "current" and touch a resource that teardown has released.
     *
     * @return false when it was already retired, so a repeated release can be reported
     *         and skipped instead of queueing a duplicate teardown
     */
    public boolean markRetired() {
        synchronized (refreshLock) {
            if (retired) return false;
            retired = true;
            cancelled = true;
            readinessEpoch++;
            refreshRequestEpoch++;
            refreshAllowed = false;
        }
        return true;
    }

    public boolean isRetired() {
        return retired;
    }
}
