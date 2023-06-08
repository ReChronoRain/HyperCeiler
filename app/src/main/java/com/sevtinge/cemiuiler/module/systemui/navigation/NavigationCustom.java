package com.sevtinge.cemiuiler.module.systemui.navigation;

import com.sevtinge.cemiuiler.module.base.BaseHook;

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
            log(String.valueOf(e));
        }
        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_height_landscape", mNavigationHeightLand);
        } catch (Exception e) {
            log(String.valueOf(e));
        }
        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_frame_height", mNavigationFrameHeight);
        } catch (Exception e) {
            log(String.valueOf(e));
        }
        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_bar_frame_height_landscape", mNavigationFrameHeightLand);
        } catch (Exception e) {
            log(String.valueOf(e));
        }

    }
}
