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
package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

/**
 * Hides the drawn gesture line without touching gesture behaviour.
 *
 * <p>The launcher side of "hide gesture line" (HideNavigationBar) works on
 * NavStubView#mHideGestureLine, but that field feeds isImmersive, getHotSpaceHeight,
 * isNeedAdjustTouchArea and canPerformQuickSwitch, and NavStubView#setHideGestureLine also
 * relayouts the gesture window. Driving it therefore changes the size of the gesture hot
 * space, which breaks swipe navigation and the long press that starts Circle to Search.
 *
 * <p>The line itself is drawn by SystemUI from dimen/navigation_handle_radius using
 * color/navigation_bar_home_handle_{light,dark}_color, the same resources the "customize
 * gesture line" option overrides. Only the colours are replaced here: a fully transparent
 * handle cannot be seen, while its size and therefore every touch region stay exactly as
 * the system computed them.
 *
 * <p>dimen/navigation_handle_radius is deliberately left alone. Forcing it to zero also
 * hides the line, but it feeds hit testing as well as painting, and a zero sized handle
 * stops the long press that starts Circle to Search (measured on HyperOS 3.3). Keeping the
 * stock radius keeps that gesture working.
 */
public class HideGestureLine extends BaseHook {
    private static final int TRANSPARENT = 0;

    @Override
    public void init() {
        setObjectReplacement("com.android.systemui", "color",
            "navigation_bar_home_handle_dark_color", TRANSPARENT);
        setObjectReplacement("com.android.systemui", "color",
            "navigation_bar_home_handle_light_color", TRANSPARENT);
    }
}
