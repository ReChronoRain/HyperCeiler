package com.sevtinge.cemiuiler.module.systemui.navigation;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class HandleLineCustom extends BaseHook {
    @Override
    public void init() {

        float mNavigationHandleRadius = ((float) mPrefsMap.getInt("system_ui_navigation_handle_custom_height", 185) / 100);
        float mNavigationHomeHandleWidth = ((float) mPrefsMap.getInt("system_ui_navigation_handle_custom_width", 145));
        float mNavigationHomeHandleWidthLand = (float) mPrefsMap.getInt("system_ui_navigation_handle_custom_width_land", 254);

        log(String.valueOf(mPrefsMap.getInt("system_ui_navigation_handle_custom_width", 145)));
        log(String.valueOf(mNavigationHomeHandleWidth));


        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_handle_radius", mNavigationHandleRadius);
        } catch (Exception e) {
            log(String.valueOf(e));
        }
        try {
        mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_home_handle_width", 666.0f);
        } catch (Exception e) {
            log(String.valueOf(e));
        }
        try {
        //写法1
        //mResHook.setObjectReplacement("com.android.systemui", "dimen_land", "navigation_home_handle_width", mNavigationHomeHandleWidthLand);
        } catch (Exception e) {
            log(String.valueOf(e));
        }
        //写法2
        //mResHook.setDensityReplacement("com.android.systemui", "dimen_land", "navigation_home_handle_width", mNavigationHomeHandleWidthLand);

        //mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_handle_horizontal_margin",  3);
        //mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_handle_sample_horizontal_margin",  3);
    }
}
