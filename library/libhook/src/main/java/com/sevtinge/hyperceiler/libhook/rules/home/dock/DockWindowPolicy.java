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

/** Pure selection/geometry rules, shared with the host-side regression test. */
public final class DockWindowPolicy {
    private DockWindowPolicy() {}

    /** OS4 offers only system material (1) and glass (3); preserve their saved IDs. */
    public static int normalizeBackgroundMode(int mode) {
        return mode == DockGlassPreset.MODE ? DockGlassPreset.MODE : 1;
    }

    public static boolean isLauncherWindow(String owner, String title, int type, int displayId) {
        // Never decorate starting windows, settings, recents or another application's window.
        return "com.miui.home".equals(owner) && displayId == 0 && (type == 1 || type == 2)
            && ("com.miui.home/com.miui.home.launcher.Launcher".equals(title)
                || "com.miui.home/.launcher.Launcher".equals(title)
                || "com.miui.home.launcher.Launcher".equals(title));
    }

    public record Bounds(int x, int y, int width, int height, float radius) {}

    public static Bounds layout(int width, int height, float density, int heightDp,
                                int marginDp, int bottomDp, int radiusDp) {
        if (width <= 0 || height <= 0 || !Float.isFinite(density) || density <= 0) return null;
        int margin = Math.min(Math.round(Math.max(0, Math.min(150, marginDp)) * density), width / 2);
        int bottom = Math.min(Math.round(Math.max(0, Math.min(150, bottomDp)) * density), height);
        int dockWidth = width - margin * 2;
        int dockHeight = Math.min(Math.round(Math.max(0, Math.min(300, heightDp)) * density), height - bottom);
        if (dockWidth <= 0 || dockHeight <= 0) return null;
        float radius = Math.min(Math.max(0, Math.min(60, radiusDp)) * density,
            Math.min(dockWidth, dockHeight) / 2f);
        return new Bounds(margin, height - bottom - dockHeight, dockWidth, dockHeight, radius);
    }
}
