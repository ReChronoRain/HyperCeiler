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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.model

import android.graphics.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobileClass.statusBarMobileClass
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.bold
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.fontSize
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.getLocation
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.leftMargin
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.rightMargin
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.verticalOffset
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.*
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import de.robv.android.xposed.*

object MobileTypeSingleHook : BaseHook() {
    override fun init() {
        getMobileViewForHyperOS()
    }

    private fun getMobileViewForHyperOS() {
        statusBarMobileClass.methodFinder()
            .filterByName("fromContext")
            .filterByParamCount(2)
            .single().createHook {
                after {
                    val mobileLeftContainer =
                        XposedHelpers.getObjectField(it.result, "mMobileLeftContainer") as ViewGroup
                    val mobileGroup =
                        XposedHelpers.getObjectField(it.result, "mMobileGroup") as LinearLayout
                    val mobileTypeSingle =
                        XposedHelpers.getObjectField(it.result, "mMobileTypeSingle") as TextView

                    setMobileType(mobileGroup, mobileTypeSingle, mobileLeftContainer)
                }
            }
    }

    private fun setMobileType(
        mobileGroup: LinearLayout,
        mobileTypeSingle: TextView,
        mobileLeftContainer: ViewGroup
    ) {
        // 移动网络类型图标显示位置
        if (!getLocation) {
            mobileGroup.removeView(mobileTypeSingle)
            mobileGroup.addView(mobileTypeSingle)
            mobileGroup.removeView(mobileLeftContainer)
            mobileGroup.addView(mobileLeftContainer)
        }

        // 自定义样式
        if (fontSize != 27) {
            mobileTypeSingle.textSize = fontSize * 0.5f
        }
        if (bold) {
            mobileTypeSingle.typeface = Typeface.DEFAULT_BOLD
        }

        val marginLeft =
            dp2px(leftMargin * 0.5f)

        val marginRight =
            dp2px(rightMargin * 0.5f)

        var topMargin = 0
        if (verticalOffset != 8) {
            val marginTop =
                dp2px((verticalOffset - 8) * 0.5f)
            topMargin = marginTop
        }

        mobileTypeSingle.setPaddingRelative(marginLeft, topMargin, marginRight, 0)
    }
}
