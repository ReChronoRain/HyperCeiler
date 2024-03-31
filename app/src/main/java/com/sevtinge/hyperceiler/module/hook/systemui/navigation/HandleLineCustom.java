package com.sevtinge.hyperceiler.module.hook.systemui.navigation;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HandleLineCustom extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        float mNavigationHandleRadius = (float) mPrefsMap.getInt("system_ui_navigation_handle_custom_thickness", 185) / 100;
        try {
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_handle_radius", mNavigationHandleRadius);
        } catch (Exception e) {
            logE(TAG, e.toString());
        }
        int mNavigationHandleLightColor =
                mPrefsMap.getInt("system_ui_navigation_handle_custom_color", -872415232);
        int mNavigationHandleDarkColor =
                mPrefsMap.getInt("system_ui_navigation_handle_custom_color_dark", -1);
        mResHook.setObjectReplacement("com.android.systemui", "color",
                "navigation_bar_home_handle_dark_color", mNavigationHandleDarkColor);
        mResHook.setObjectReplacement("com.android.systemui", "color",
                "navigation_bar_home_handle_light_color", mNavigationHandleLightColor);
    }
}
