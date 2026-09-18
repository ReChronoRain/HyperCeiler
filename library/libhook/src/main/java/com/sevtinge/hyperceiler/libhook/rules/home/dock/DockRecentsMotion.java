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

/** Bounded, interruptible motion for our background, not a transform of the launcher. */
public final class DockRecentsMotion {
    public static final String WALLPAPER_ACTION = "miui.wallpaper.animation";
    // A deliberately small initial lift. The native icon transform is not exported by HYOS.
    public static final float LIFT_DP = 20f;
    // OS4 LauncherBlurGetxController presets observed during home -> recents -> home.
    // These synchronize the hold/overview backdrop, not the earlier Flutter drag transform.
    public static final double ENTER_RESPONSE = 0.3;
    public static final double ENTER_DAMPING = 0.9;
    public static final double EXIT_RESPONSE = 0.45;
    public static final double EXIT_DAMPING = 0.95;
    public static final long MAX_DURATION_MS = 1500;
    // bIa wallpaper presets in the OS4 APK combine scene zoom with a 1.05 base.
    // Keep the unscaled profile for older senders; do not infer a scene from a range.
    private static final double[] BASE_SCALES = {1.05, 1.0};
    private double from;
    private double target;
    private double initialVelocity;
    private long startedAt;
    private record Sample(double position, double velocity) { }

    /** A VSync timestamp can precede a newer scene/layout callback; never rewind motion. */
    public static long frameTimeMillis(long frameTimeNanos, long previousTimeMillis) {
        return Math.max(Math.max(0, previousTimeMillis), frameTimeNanos / 1_000_000L);
    }

    /** Only recognize the scene endpoints found in this OS4 launcher, never arbitrary zoom. */
    public static Boolean overviewTarget(String command, String action, double scale) {
        if (!WALLPAPER_ACTION.equals(command) || !("startAnim".equals(action) || "setTo".equals(action))
                || !Double.isFinite(scale)) return null;
        for (double base : BASE_SCALES) {
            double scene = scale / base;
            if (Math.abs(scene - 1.06) < 0.0001) return true;
            if (Math.abs(scene - 1.0) < 0.0001 || Math.abs(scene - 1.14) < 0.0001
                    || Math.abs(scene - 1.18) < 0.0001) return false;
        }
        return null;
    }

    /** Exact home preset only; app-launch and home-hide presets must not rearm glass. */
    public static boolean homeTarget(String command, String action, double scale) {
        if (!WALLPAPER_ACTION.equals(command) || !("startAnim".equals(action) || "setTo".equals(action))
                || !Double.isFinite(scale)) return false;
        for (double base : BASE_SCALES) {
            if (Math.abs(scale / base - 1.0) < 0.0001) return true;
        }
        return false;
    }

    /** App-launch/home-hide zoom, before the launcher becomes invisible or rotates. */
    public static boolean leavingHomeTarget(String command, String action, double scale) {
        if (!WALLPAPER_ACTION.equals(command) || !("startAnim".equals(action) || "setTo".equals(action))
                || !Double.isFinite(scale)) return false;
        for (double base : BASE_SCALES) {
            double scene = scale / base;
            if (Math.abs(scene - 1.14) < 0.0001 || Math.abs(scene - 1.18) < 0.0001) return true;
        }
        return false;
    }

    public boolean setOverview(boolean overview, long now) {
        double next = overview ? 1 : 0;
        if (target == next) return false; // Repeated commands must not restart the animation.
        Sample current = sample(now);
        from = current.position;
        initialVelocity = current.velocity;
        target = next;
        startedAt = now;
        return true;
    }

    public float progress(long now) {
        return (float) sample(now).position;
    }

    /** Resume the fallback from the last native position if its transport disappears. */
    public void resumeFrom(float progress, boolean overview, long now) {
        from = Float.isFinite(progress) ? Math.max(0, Math.min(1, progress)) : 0;
        target = overview ? 1 : 0;
        initialVelocity = 0;
        startedAt = now;
    }

    /** Normalized units per second, retained across rapid target reversals. */
    public double velocity(long now) {
        return sample(now).velocity;
    }

    private Sample sample(long now) {
        long elapsed = Math.max(0, now - startedAt);
        if ((from == target && initialVelocity == 0) || elapsed >= MAX_DURATION_MS) {
            return new Sample(target, 0);
        }
        double response = target == 1 ? ENTER_RESPONSE : EXIT_RESPONSE;
        double damping = target == 1 ? ENTER_DAMPING : EXIT_DAMPING;
        double omega = 2 * Math.PI / response;
        double decay = damping * omega;
        double frequency = omega * Math.sqrt(1 - damping * damping);
        double t = elapsed / 1000.0;
        double displacement = from - target;
        double sineCoefficient = (initialVelocity + decay * displacement) / frequency;
        double cos = Math.cos(frequency * t);
        double sin = Math.sin(frequency * t);
        double envelope = Math.exp(-decay * t);
        double position = target + envelope * (displacement * cos + sineCoefficient * sin);
        double speed = envelope * ((sineCoefficient * frequency - decay * displacement) * cos
            - (displacement * frequency + decay * sineCoefficient) * sin);
        // Clamp only our small background lift; never overshoot the configured endpoints.
        if (position < 0 || position > 1) {
            double bounded = Math.max(0, Math.min(1, position));
            if (bounded == target) finish();
            return new Sample(bounded, 0);
        }
        // Stop frame requests at a subpixel error, with an independent hard deadline.
        if (Math.abs(position - target) < 0.001 && Math.abs(speed) < 0.01) {
            finish();
            return new Sample(target, 0);
        }
        return new Sample(position, speed);
    }

    public boolean isRunning(long now) {
        Sample current = sample(now);
        return current.position != target || current.velocity != 0;
    }

    public float offsetY(float density, int baseY, long now) {
        if (!Float.isFinite(density) || density <= 0 || baseY <= 0) return 0;
        return -Math.min(LIFT_DP * density, baseY) * progress(now);
    }

    /** Hidden windows need no animation frames; retain the latest scene endpoint. */
    public void finish() {
        from = target;
        initialVelocity = 0;
    }
}
