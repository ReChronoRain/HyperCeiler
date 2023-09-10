package com.sevtinge.cemiuiler.module.hook.systemui.navigation

import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam

object HandleLineCustom : BaseHook() {
    override fun init() {
        val mNavigationHandleRadius =
            mPrefsMap.getInt("system_ui_navigation_handle_custom_thickness", 185).toFloat() / 100
        try {
            mResHook.setDensityReplacement(
                "com.android.systemui", "dimen", "navigation_handle_radius", mNavigationHandleRadius
            )
        } catch (e: Exception) {
            logI(e.toString())
        }
        /*
        val mNavigationHandleHeight =
            mPrefsMap.getInt("system_ui_navigation_handle_custom_thickness", 600).toFloat() / 10
        val mNavigationHomeHandleWidth =
            mPrefsMap.getInt("system_ui_navigation_handle_custom_width", 145).toFloat()
        val mNavigationHomeHandleWidthLand =
            mPrefsMap.getInt("system_ui_navigation_handle_custom_width_land", 254).toFloat()
        try {
            mResHook.setDensityReplacement(
                "com.android.systemui",
                "dimen",
                "navigation_handle_bottom",
                mNavigationHandleHeight
            );
        } catch (e: Exception) {
            log(e.toString())
        }

         */

        /*
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
        }*/
        // 写法2
        // mResHook.setDensityReplacement("com.android.systemui", "dimen_land", "navigation_home_handle_width", mNavigationHomeHandleWidthLand);

        // mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_handle_horizontal_margin",  3);
        // mResHook.setDensityReplacement("com.android.systemui", "dimen", "navigation_handle_sample_horizontal_margin",  3);
    }

    fun initResource(resParam: InitPackageResourcesParam) {
        val mNavigationHandleLightColor =
            mPrefsMap.getInt("system_ui_navigation_handle_custom_color", -872415232)
        val mNavigationHandleDarkColor =
            mPrefsMap.getInt("system_ui_navigation_handle_custom_color_dark", -1)
        logI("mNavigationHandleLightColor is $mNavigationHandleLightColor")
        logI("mNavigationHandleDarkColor is $mNavigationHandleDarkColor")
        resParam.res.setReplacement(
            "com.android.systemui",
            "color",
            "navigation_bar_home_handle_dark_color",
            mNavigationHandleLightColor
        )
        resParam.res.setReplacement(
            "com.android.systemui",
            "color",
            "navigation_bar_home_handle_light_color",
            mNavigationHandleDarkColor
        )
    }
}
