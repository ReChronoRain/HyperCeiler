/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

/** Event-driven single-flight reads. Delays exist only for a pending edit or bounded recovery. */
final class DockGeometryRefreshGate {
    private final long intervalMs;
    private boolean attempted, queued, dirty, closed;
    private long lastStartMs;
    private int retries;

    DockGeometryRefreshGate(long intervalMs) { this.intervalMs = intervalMs; }

    /** -1 means no task; nonnegative means queue ONE read after this delay. */
    synchronized long request(long nowMs, boolean force) {
        if (closed) return -1;
        if (force) { dirty = true; retries = 0; }
        if (queued || (!force && attempted)) return -1;
        queued = true;
        return delay(nowMs);
    }

    synchronized boolean begin(long nowMs) {
        if (closed || !queued) return false;
        attempted = true; lastStartMs = nowMs; dirty = false;
        return true;
    }

    synchronized long finish(boolean success, long nowMs) {
        queued = false;
        if (closed) return -1;
        if (dirty) { queued = true; return delay(nowMs); }
        if (!success && retries < 3) {
            queued = true;
            return 1500L << retries++; // 1.5 / 3 / 6 seconds, then stop until another event.
        }
        return -1; // No idle poll, even if every provider attempt failed.
    }

    synchronized void rejected() { queued = false; }
    synchronized void close() { closed = true; queued = false; dirty = false; }
    private long delay(long nowMs) {
        return attempted ? Math.max(0, intervalMs - Math.max(0, nowMs - lastStartMs)) : 0;
    }

    static boolean isGeometryKey(String key) {
        if (key == null) return true;
        if (key.startsWith("prefs_key_")) key = key.substring("prefs_key_".length());
        return switch (key) {
            case "home_dock_bg_custom_enable", "home_dock_add_blur", "home_dock_bg_color",
                 "home_dock_bg_height", "home_dock_bg_margin_horizontal", "home_dock_bg_margin_bottom",
                 "home_dock_bg_radius", "home_other_home_mode" -> true;
            default -> false;
        };
    }
}
