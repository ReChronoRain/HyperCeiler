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
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

public class HandleLineCustom extends BaseHook {
    @Override
    public void init() {
        float mNavigationHandleRadius = (float) PrefsBridge.getInt("system_ui_navigation_handle_custom_thickness", 185) / 100;
        try {
            setDensityReplacement("com.android.systemui", "dimen", "navigation_handle_radius", mNavigationHandleRadius);
        } catch (Exception e) {
            XposedLog.w(TAG, getPackageName(), e.toString());
        }
        int mNavigationHandleLightColor =
                PrefsBridge.getInt("system_ui_navigation_handle_custom_color", -872415232);
        int mNavigationHandleDarkColor =
                PrefsBridge.getInt("system_ui_navigation_handle_custom_color_dark", -1);
        setObjectReplacement("com.android.systemui", "color",
                "navigation_bar_home_handle_dark_color", mNavigationHandleLightColor);
        setObjectReplacement("com.android.systemui", "color",
                "navigation_bar_home_handle_light_color", mNavigationHandleDarkColor);
    }
}
