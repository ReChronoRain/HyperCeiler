/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockRotationPolicy;

/**
 * Regression checks for the rotation gate that suspends wallpaper sampling.
 *
 * <p>The device case behind this test: a landscape game leaves the display rotated while the
 * launcher - and therefore the Dock layer - stays a portrait window. The glass then samples a
 * transposed region and keeps that colour because the geometry never changes, so the gate has to
 * come from the display rotation and the returning cycle has to bump the host key.
 */
public final class DockRotationPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        DockRotationPolicy policy = new DockRotationPolicy();

        // Portrait from the start: nothing is suspended and no settle window is armed, so a
        // freshly booted Dock still builds its first host immediately.
        check(!policy.update(false, 0L), "the natural rotation is not a transition");
        check(!policy.isRotated(), "portrait must not suspend the glass");
        check(!policy.isSettling(0L), "portrait at boot must not delay the first host");
        check(policy.settleRemainingMs(0L) == 0L, "no settle window before a cycle");
        check("0".equals(policy.keyFragment()), "the initial key fragment is stable");

        // Rotating away suspends sampling and cancels any settle window.
        check(policy.update(true, 10_000L), "entering a rotated display is a transition");
        check(policy.isRotated(), "a rotated display must suspend the glass");
        check(!policy.mayPresent(true, false), "a live unpaused source stays hidden during rotation");
        check(!policy.mayPresent(false, true), "an unready paused source cannot be displayed");
        check(policy.mayPresent(true, true), "retained glass follows a visible returning launcher");
        check(!policy.isSettling(10_000L), "no settling while still rotated");
        check("0".equals(policy.keyFragment()), "rotating away must not bump the key on its own");
        check(!policy.update(true, 11_000L), "a repeated reading is not a transition");

        // Returning arms the settle window and bumps the key exactly once.
        check(policy.update(false, 12_000L), "returning to portrait is a transition");
        check(!policy.isRotated(), "portrait resumes sampling");
        check(policy.isSettling(12_000L), "the settle window starts on return");
        check(policy.isSettling(12_799L), "the settle window covers its whole span");
        check(!policy.isSettling(12_800L), "the settle window ends at its span");
        check(policy.settleRemainingMs(12_300L) == 500L, "the caller can schedule the resume");
        check(policy.settleRemainingMs(20_000L) == 0L, "an elapsed window leaves nothing to wait for");
        check("1".equals(policy.keyFragment()), "one cycle must produce a new host key");
        check(!policy.requiresNewCapture(0, true, true),
                "return keeps the last ready portrait texture without a material fallback");
        check(policy.requiresNewCapture(0, true, false),
                "return rebuilds a source that could have sampled the landscape display");
        check(policy.requiresNewCapture(0, false, true),
                "pause alone cannot qualify an unready texture for reuse");
        check(!policy.requiresNewCapture(1, true, false),
                "resuming a retained host in the acknowledged epoch does not rebuild it");

        // A second cycle keeps counting, so two rotations can never share a key.
        policy.update(true, 30_000L);
        policy.update(false, 31_000L);
        check("2".equals(policy.keyFragment()), "every cycle must produce a new host key");

        // The suspend decision survives an unreadable rotation: callers pass the last value back
        // in, so a missing display must not silently resume sampling on a rotated screen.
        check(policy.isRotated() == false, "the test only reaches here in portrait");
        policy.update(true, 40_000L);
        check(policy.isRotated() && !policy.isSettling(40_000L), "still suspended while unreadable");
        policy.update(false, 41_000L);
        check(!policy.isRotated() && policy.isSettling(41_000L), "recovery resumes with a settle");

        System.out.println("DockRotationPolicy tests passed");
    }
}
