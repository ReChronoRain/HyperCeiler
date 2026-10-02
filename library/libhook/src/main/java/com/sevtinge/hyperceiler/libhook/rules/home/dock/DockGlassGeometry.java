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
 * The rotation contract between the display and the vendor's pass-window blur geometry.
 *
 * <p>The glass host is a windowless {@code SurfaceControlViewHost}. When the display rotates,
 * the framework never sends it a relayout, so {@code ViewRootImpl.checkConfigRot()} is the only
 * thing that can refresh {@code mConfigRot} - and it only runs on the pause/resume, scale-change
 * or texture-creation chains. A host that survived a rotation therefore keeps sampling the
 * wallpaper through the rotated transform until something walks that chain again: the visible
 * "the sample position changed" artefact.
 *
 * <p>{@code ViewRootImpl.checkConfigRot()} stores {@code (installOrientation + rotation) % 4},
 * so the expected value of {@code mConfigRot} is derived the same way here. Android-free, so
 * the host regression suite can pin the comparison without a device.
 */
public final class DockGlassGeometry {
    private DockGlassGeometry() {}

    /**
     * The {@code mConfigRot} the vendor produces for a display state, or {@code -1} when the
     * rotation is unknown and no conclusion can be drawn.
     */
    public static int expectedConfigRotation(int displayRotation, int installOrientation) {
        if (displayRotation < 0) return -1;
        int install = installOrientation < 0 ? 0 : installOrientation;
        return Math.floorMod(install + displayRotation, 4);
    }

    /**
     * True when the vendor's blur geometry can be trusted for the current display.
     *
     * <p>Fail-open by design: a missing field, an unresolvable display or an older host must
     * never block a glass that works. Only a known, conflicting pair is reported as stale.
     */
    public static boolean matches(int expectedConfigRotation, int configRotation) {
        if (expectedConfigRotation < 0 || configRotation < 0) return true;
        return expectedConfigRotation == configRotation;
    }
}
