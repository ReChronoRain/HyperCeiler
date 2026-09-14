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

/**
 * Decides when the Dock must stop sampling the wallpaper because the display is rotated.
 *
 * <p>The Dock layer lives inside the launcher's window stack while the compositor feeds the
 * glass its cross-window blur source in display space. As soon as the display is rotated the
 * source and the layer's own geometry disagree, and the panel is drawn with a transposed,
 * visibly wrong colour sample - and it keeps it, because a rotation does not change the
 * launcher window's frame and therefore never changes the glass key that would rebuild the host.
 *
 * <p>The launcher's own frame cannot see this. On this ROM the home screen is a portrait window
 * that the display rotates together with, so a landscape display still reports a portrait
 * frame - verified on-device while a landscape game was in the foreground. Reading
 * {@code Configuration.orientation} is not an option either: it flips while the display is
 * still portrait, which is what made an earlier attempt hide the Dock at random.
 *
 * <p>Android-free so the host regression suite can drive every transition without a device.
 *
 * <p>Threading: the launcher traversal (display thread) and the one-second sweep (WMS handler)
 * both feed {@link #update}, so every method is synchronized. The state is two fields and a
 * counter, and callers run at most a few times per second.
 */
public final class DockRotationPolicy {
    /**
     * How long full-opacity glass stays off after the display returns to its natural rotation.
     *
     * <p>The new host starts capturing during the return animation. Keep the compositor
     * fallback visible until the wallpaper and launcher have had time to settle.
     */
    private static final long SETTLE_MS = 800L;

    private boolean rotated;
    private int epoch;
    private long settledAt = -1L;

    /**
     * Record the display's rotation.
     *
     * @param rotated  true while the display is away from its natural orientation
     * @param nowUptimeMillis the caller's {@code SystemClock.uptimeMillis()}
     * @return true when the state changed, so the caller can report the transition once
     */
    public synchronized boolean update(boolean rotated, long nowUptimeMillis) {
        if (rotated == this.rotated) return false;
        this.rotated = rotated;
        if (rotated) {
            settledAt = -1L;
        } else {
            // One completed cycle. The epoch goes into the glass key so the next host is built
            // from scratch instead of reusing the one that sampled the rotated background.
            epoch++;
            settledAt = nowUptimeMillis;
        }
        return true;
    }

    /** True while the display is rotated: the Dock must neither sample nor show the glass. */
    public synchronized boolean isRotated() {
        return rotated;
    }

    /** A retained portrait texture can follow the launcher through its rotated return. */
    public synchronized boolean mayPresent(boolean ready, boolean retainedCapture) {
        return !rotated || (ready && retainedCapture);
    }

    /** Rebuild a rotated source only if its last valid sample was not preserved. */
    public synchronized boolean requiresNewCapture(int hostEpoch, boolean ready, boolean retainedCapture) {
        return hostEpoch != epoch && !(ready && retainedCapture);
    }

    /** True during the brief window after the display returned to its natural rotation. */
    public synchronized boolean isSettling(long nowUptimeMillis) {
        return settledAt >= 0L && nowUptimeMillis - settledAt < SETTLE_MS;
    }

    /** Time left in that window, so the caller can schedule the traversal that resumes sampling. */
    public synchronized long settleRemainingMs(long nowUptimeMillis) {
        if (settledAt < 0L) return 0L;
        return Math.max(0L, SETTLE_MS - (nowUptimeMillis - settledAt));
    }

    /** Completed rotation cycles, for diagnostics only. */
    public synchronized int getEpoch() {
        return epoch;
    }

    /** Key fragment for the glass host: a new cycle must never reuse the previous host. */
    public synchronized String keyFragment() {
        return Integer.toString(epoch);
    }
}
