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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.dp2px
import com.sevtinge.hyperceiler.utils.devicesdk.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.utils.setObjectField
import de.robv.android.xposed.XposedHelpers

object MobileTypeSingleHook : BaseHook() {
    // 初始化开关
    private val getLocation by lazy {
        // 显示在信号左侧
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_left")
    }
    private val isOnlyShowNetwork by lazy {
        // 仅显示上网卡
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_only_show_network")
    }
    private val isShowDoulRowNetwork by lazy {
        // 是否启用了双排移动网络图标
        mPrefsMap.getBoolean("system_ui_statusbar_network_icon_enable")
    }
    private val bold by lazy {
        // 加粗
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_bold")
    }
    private val fontSize by lazy {
        // 字体大小
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_font_size", 27)
    }
    private val leftMargin by lazy {
        // 左侧间距
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_left_margin", 0)
    }
    private val rightMargin by lazy {
        // 右侧间距
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_right_margin", 0)
    }
    private val verticalOffset by lazy {
        // 上下偏移量
        mPrefsMap.getInt("system_ui_statusbar_mobile_type_vertical_offset", 8)
    }
    private val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }

    @SuppressLint("DiscouragedApi")
    override fun init() {
        // 兼容图标异常空位的问题，一些机器不需要这两个 hook
        /*val afterUpdate: MethodHook = object : MethodHook() {
            override fun after(param: MethodHookParam) {
                val mMobileLeftContainer =
                    XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer")
                XposedHelpers.callMethod(mMobileLeftContainer, "setVisibility", 8)
            }
        }
        hookAllMethods(statusBarMobileClass, "applyMobileState", afterUpdate)*/

        // 使网络类型单独显示
        statusBarMobileClass.methodFinder()
            .filterByName("applyMobileState")
            .single().createHook {
                before {
                    val mobileIconState = it.args[0]
                    mobileIconState.setObjectField("showMobileDataTypeSingle", true)
                    mobileIconState.setObjectField("fiveGDrawableId", 0)
                }
            }

        if (isMoreHyperOSVersion(1f)) {
            getMobileViewForHyperOS()
        } else {
            getMobileViewForMIUI()
        }

        showNonNetworkIcon() // 显示非上网卡的大图标
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

    private fun getMobileViewForMIUI() {
        statusBarMobileClass.methodFinder()
            .filterByName("init")
            .single().createHook {
                after {
                    val mobileLeftContainer =
                        XposedHelpers.getObjectField(it.thisObject, "mMobileLeftContainer") as ViewGroup
                    val mobileGroup =
                        XposedHelpers.getObjectField(it.thisObject, "mMobileGroup") as LinearLayout
                    val mobileTypeSingle =
                        XposedHelpers.getObjectField(it.thisObject, "mMobileTypeSingle") as TextView

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

    private fun showNonNetworkIcon() {
        if (!isOnlyShowNetwork && !isShowDoulRowNetwork) {
            statusBarMobileClass.methodFinder()
                .filterByName("updateState")
                .single().createHook {
                    after {
                        val mobileIconState = it.args[0]
                        val statusBarMobileView = it.thisObject as ViewGroup
                        val context: Context = statusBarMobileView.context
                        val res: Resources = context.resources

                        val mobileTypeSingleId: Int =
                            res.getIdentifier("mobile_type_single", "id", "com.android.systemui")
                        val mobileTypeSingle =
                            statusBarMobileView.findViewById<TextView>(mobileTypeSingleId)

                        if (!mobileIconState.getObjectFieldAs<Boolean>("dataConnected") &&
                            !mobileIconState.getObjectFieldAs<Boolean>("wifiAvailable")
                        ) {
                            mobileTypeSingle.visibility = View.VISIBLE
                        }
                    }
                }
        }
    }
}
