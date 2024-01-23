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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.navigation;

import com.sevtinge.hyperceiler.module.base.BaseHook;


public class NavigationCustom extends BaseHook {
    @Override
    public void init() {

        float mNavigationHeight = ((float) mPrefsMap.getInt("system_ui_navigation_custom_height", 100) / 10);
        float mNavigationHeightLand = ((float) mPrefsMap.getInt("system_ui_navigation_custom_height_land", 100) / 10);
        float mNavigationFrameHeight = ((float) mPrefsMap.getInt("system_ui_navigation_frame_custom_height", 100) / 10);
        float mNavigationFrameHeightLand = ((float) mPrefsMap.getInt("system_ui_navigation_frame_custom_height_land", 100) / 10);

        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_height", mNavigationHeight);
        } catch (Exception e) {
            logE(TAG, this.lpparam.packageName, "navigation_bar_height error", e);
        }
        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_height_landscape", mNavigationHeightLand);
        } catch (Exception e) {
            logE(TAG, this.lpparam.packageName, "navigation_bar_height_landscape error", e);
        }
        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_frame_height", mNavigationFrameHeight);
        } catch (Exception e) {
            logE(TAG, this.lpparam.packageName, "navigation_bar_frame_height error", e);
        }
        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_frame_height_landscape", mNavigationFrameHeightLand);
        } catch (Exception e) {
            logE(TAG, this.lpparam.packageName, "navigation_bar_frame_height_landscape error", e);
        }

    }
}
