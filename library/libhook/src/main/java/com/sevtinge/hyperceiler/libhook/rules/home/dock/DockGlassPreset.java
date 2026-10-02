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
 * OS4's 42-float HWUI glass ABI, carrying the glass the launcher draws behind a folder ICON
 * ({@code FolderBlurUtils.buildFolderGlass}) - not the control-center card token and not the
 * expanded folder panel. See tests/home-dock-window/OS4_GLASS_NOTES.md.
 */
public final class DockGlassPreset {
    // New value: do not reuse 2, which means legacy custom blur on older systems.
    public static final int MODE = 3;
    public static final int MATERIAL_TYPE = 1;
    // The folder token's blur pair. Flutter and HWUI do not share a blur unit, so these are the
    // token's own configured radii carried across unchanged; verify the result on-device.
    public static final int SMALL_BLUR_RADIUS = 36;
    public static final int BIG_BLUR_RADIUS = 500;
    public static final int GLASS_ENHANCE_FLAG = 0x2000;

    private DockGlassPreset() {}

    /** Fresh arrays: native setters and callers must never mutate a shared preset. */
    public static float[] parameters(boolean lightWallpaper) {
        // FolderBlurUtils.buildFolderGlass selects Medium_Thin_High on a light
        // wallpaper, Medium_Thin_Low otherwise. This is NOT UI night mode.
        // Values are translated from the launcher's GlassToken groups into the
        // HWUI ABI; never use Dart object offsets as runtime memory addresses.
        if (lightWallpaper) return new float[]{
            .5f, 1.6f, 0f, .3f, .2f, 1.5f, .24f, .1f, .6f, .8f,
            0f, 1f, 1f, 1f, .1f, 0f, .3f, 1.2f, 1f,
            36f, 2.2f, 100f, 200f, 1f, .6f, -.4f, .6f, -.8f,
            1.2f, .6f, 1.4f, 1.15f, 3f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
        };
        return new float[]{
            .5f, 1f, 0f, 1f, .16f, 1.4f, .21f, .12f, .6f, .8f,
            0f, 1f, 1f, 1f, .1f, 0f, .2f, 1.4f, 1f,
            36f, 2.2f, 100f, 200f, 1f, .6f, -.4f, .6f, -.8f,
            1.5f, 1f, 1.4f, 1.15f, 2f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
        };
    }

    public static int fallbackColor(boolean dark) {
        return dark ? 0x18505050 : 0x18FFFFFF;
    }
}
