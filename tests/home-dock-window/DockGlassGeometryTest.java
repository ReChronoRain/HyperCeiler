/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockGlassGeometry;

/**
 * Regression checks for the display/vendor blur-rotation contract.
 *
 * <p>The device case behind this test: the glass host is a windowless
 * {@code SurfaceControlViewHost}, so the framework never sends it a relayout. After a landscape
 * app returns to the portrait launcher, {@code mConfigRot} still carries the rotated transform
 * unless the relayout math is walked again, and every sampled frame is then mapped to the wrong
 * wallpaper region - the "the colour moved" artefact. The host compares the vendor value against
 * the live display before it lets the panel present a frame.
 */
public final class DockGlassGeometryTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // The vendor stores (installOrientation + rotation) % 4, exactly like checkConfigRot.
        check(DockGlassGeometry.expectedConfigRotation(0, 0) == 0, "portrait maps to 0");
        check(DockGlassGeometry.expectedConfigRotation(1, 0) == 1, "landscape 90 maps to 1");
        check(DockGlassGeometry.expectedConfigRotation(2, 0) == 2, "reverse portrait maps to 2");
        check(DockGlassGeometry.expectedConfigRotation(3, 0) == 3, "landscape 270 maps to 3");
        check(DockGlassGeometry.expectedConfigRotation(0, 1) == 1,
                "a non-zero install orientation is folded in");
        check(DockGlassGeometry.expectedConfigRotation(3, 1) == 0,
                "install orientation wraps at four");

        // Unknown rotations carry no conclusion.
        check(DockGlassGeometry.expectedConfigRotation(-1, 0) == -1,
                "an unreadable display has no expected geometry");
        check(DockGlassGeometry.expectedConfigRotation(0, -1) == 0,
                "an unreadable install orientation defaults to natural");
        check(DockGlassGeometry.matches(-1, 3), "fail open when the display is unknown");
        check(DockGlassGeometry.matches(0, -1), "fail open when the vendor field is absent");

        // The actual gate: only a known, conflicting pair is stale.
        check(DockGlassGeometry.matches(0, 0), "matching geometry is valid");
        check(!DockGlassGeometry.matches(0, 1),
                "a rotated vendor transform must be reported stale after returning to portrait");
        check(!DockGlassGeometry.matches(1, 0),
                "a portrait vendor transform must be reported stale on a rotated display");
        check(DockGlassGeometry.matches(3, 3), "landscape 270 keeps matching");

        System.out.println("DockGlassGeometry tests passed");
    }
}
