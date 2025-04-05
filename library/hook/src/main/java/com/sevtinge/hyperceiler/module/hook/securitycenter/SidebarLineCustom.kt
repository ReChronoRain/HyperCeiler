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
package com.sevtinge.hyperceiler.module.hook.securitycenter

import com.sevtinge.hyperceiler.module.base.BaseHook

object SidebarLineCustom : BaseHook() {

    override fun init() {
        val mSidebarLineColorDefault =
            mPrefsMap.getInt("security_center_sidebar_line_color_default", -1294740525)
        val mSidebarLineColorDark =
            mPrefsMap.getInt("security_center_sidebar_line_color_dark", -6842473)
        val mSidebarLineColorLight =
            mPrefsMap.getInt("security_center_sidebar_line_color_light", -872415232)
        logI(
            TAG,
            "com.miui.securitycenter",
            "mSidebarLineColorDefault is $mSidebarLineColorDefault"
        )
        logI(TAG, "com.miui.securitycenter", "mSidebarLineColorDark is $mSidebarLineColorDark")
        logI(TAG, "com.miui.securitycenter", "mSidebarLineColorLight is $mSidebarLineColorLight")
        mResHook.setResReplacement(
            "com.miui.securitycenter",
            "color",
            "sidebar_line_color",
            mSidebarLineColorDefault
        )
        mResHook.setResReplacement(
            "com.miui.securitycenter",
            "color",
            "sidebar_line_color_dark",
            mSidebarLineColorLight
        )
        mResHook.setResReplacement(
            "com.miui.securitycenter",
            "color",
            "sidebar_line_color_light",
            mSidebarLineColorDark
        )
    }
/*
    fun initResource(resParam: XC_InitPackageResources.InitPackageResourcesParam) {
        val mSidebarLineColorDefault =
            mPrefsMap.getInt("security_center_sidebar_line_color_default", -1294740525)
        val mSidebarLineColorDark =
            mPrefsMap.getInt("security_center_sidebar_line_color_dark", -6842473)
        val mSidebarLineColorLight =
            mPrefsMap.getInt("security_center_sidebar_line_color_light", -872415232)
        logI(
            TAG,
            "com.miui.securitycenter",
            "mSidebarLineColorDefault is $mSidebarLineColorDefault"
        )
        logI(TAG, "com.miui.securitycenter", "mSidebarLineColorDark is $mSidebarLineColorDark")
        logI(TAG, "com.miui.securitycenter", "mSidebarLineColorLight is $mSidebarLineColorLight")
        resParam.res.setReplacement(
            "com.miui.securitycenter",
            "color",
            "sidebar_line_color",
            mSidebarLineColorDefault
        )
        resParam.res.setReplacement(
            "com.miui.securitycenter",
            "color",
            "sidebar_line_color_dark",
            mSidebarLineColorLight
        )
        resParam.res.setReplacement(
            "com.miui.securitycenter",
            "color",
            "sidebar_line_color_light",
            mSidebarLineColorDark
        )
    }*/


}
