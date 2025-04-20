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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.utils;

import android.view.View;

import fan.core.utils.MiuiBlurUtils;

public class BlurUtils {

    public static void setupLogoBlur(View view) {
        setupViewBlur(view, new int[]{-867546550, -11579569, -15011328}, new int[]{19, 100, 106});
    }

    public static void setupViewBlur(View view, int[] blendColors, int[] iArr2) {
        setupViewBlur(view, true, blendColors, iArr2);
    }

    public static void setupViewBlur(View view, boolean isEnabled, int[] blendColors, int[] iArr2) {
        if (view != null) {
            if (isEnabled) {
                MiuiBlurUtils.setViewBlurMode(view, 3);
                for (int i = 0; i < blendColors.length; i++) {
                    MiuiBlurUtils.addBackgroundBlenderColor(view, blendColors[i], iArr2[i]);
                }
            } else {
                MiuiBlurUtils.setViewBlurMode(view, 0);
                MiuiBlurUtils.clearBackgroundBlenderColor(view);
            }
        }
    }
}
