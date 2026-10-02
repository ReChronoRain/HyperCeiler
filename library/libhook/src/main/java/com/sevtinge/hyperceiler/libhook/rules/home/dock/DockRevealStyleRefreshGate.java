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

/** Single-flight provider reads shared by WMS and the existing IPC worker. No timer is owned. */
final class DockRevealStyleRefreshGate {
    private final long intervalMs;
    private boolean hasRead;
    private long lastReadMs;
    private boolean queued;
    private boolean forcePending;

    DockRevealStyleRefreshGate(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    synchronized boolean request(long nowMs, boolean force) {
        if (force) forcePending = true;
        if (queued) return false;
        if (!forcePending && hasRead && nowMs - lastReadMs < intervalMs) return false;
        queued = true;
        return true;
    }

    /** Events before the read starts are covered by this read; later events need another read. */
    synchronized void begin(long nowMs) {
        lastReadMs = nowMs;
        hasRead = true;
        forcePending = false;
    }

    /** Always release the latch, including query failure and a rejected Handler.post(). */
    synchronized boolean finish() {
        queued = false;
        return forcePending;
    }
}
