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

import android.annotation.*
import android.graphics.*
import android.graphics.drawable.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.*
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import com.sevtinge.hyperceiler.utils.log.*
import de.robv.android.xposed.*
import de.robv.android.xposed.XC_MethodHook.*

@SuppressLint("StaticFieldLeak")
object MobileTypeSingleHook : BaseHook() {
    // 初始化开关
    private val getLocation by lazy {
        // 显示在信号左侧
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_left")
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

    private val isShowPaw by lazy {
        // 显示爪爪
        mPrefsMap.getBoolean("system_ui_status_bar_icon_paw")
    }

    private val isShowMobileType by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable")
    }

    private val mobileSignalControllerClass by lazy {
        loadClass("com.android.systemui.statusbar.connectivity.MobileSignalController")
    }

    private val qpt by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0)
    }

    private val hideIndicator by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator")
    }

    private val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }

    private var mMobileType: View? = null
    private var dataConnected: Boolean = false
    private var wifiAvailable: Boolean = false

    override fun init() {

        hookAllMethods(statusBarMobileClass, "updateState", object : MethodHook() {
            override fun after(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator || isShowPaw) {
                    getMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
            }
        })

        hookAllMethods(statusBarMobileClass, "applyMobileState", object : MethodHook() {
            override fun after(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator || isShowPaw) {
                    getMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
            }
        })


        // 兼容图标异常空位的问题，一些机器不需要这两个 hook
        /*val afterUpdate: MethodHook = object : MethodHook() {
            override fun after(param: MethodHookParam) {
                val mMobileLeftContainer =
                    XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer")
                XposedHelpers.callMethod(mMobileLeftContainer, "setVisibility", 8)
            }
        }
        hookAllMethods(statusBarMobileClass, "applyMobileState", afterUpdate)*/

        if (isMoreHyperOSVersion(1f)) {
            getMobileViewForHyperOS()
        } else {
            getMobileViewForMIUI()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun getMobileType(param: MethodHookParam) {
        mMobileType = XposedHelpers.getObjectField(param.thisObject, "mMobileType") as View
        dataConnected = XposedHelpers.getObjectField(param.args[0], "dataConnected") as Boolean
        wifiAvailable = XposedHelpers.getObjectField(param.args[0], "wifiAvailable") as Boolean
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
                    val mLight =
                        XposedHelpers.getObjectField(it.result, "mLight") as Boolean

                    setMobileType(mobileGroup, mobileTypeSingle, mobileLeftContainer, mLight)
                }
            }



        findAndHookMethod(
            statusBarMobileClass,
            "onDarkChanged",
            ArrayList::class.java,
            Float::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val mobileLeftContainer =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileLeftContainer"
                        ) as ViewGroup
                    val mobileGroup =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileGroup"
                        ) as LinearLayout
                    val mobileTypeSingle =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileTypeSingle"
                        ) as TextView
                    val mLight =
                        XposedHelpers.getObjectField(param?.thisObject, "mLight") as Boolean

                    setMobileType(mobileGroup, mobileTypeSingle, mobileLeftContainer, mLight)
                }
            })

        findAndHookMethod(
            statusBarMobileClass,
            "updateState",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy\$MobileIconState",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val mobileLeftContainer =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileLeftContainer"
                        ) as ViewGroup
                    val mobileGroup =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileGroup"
                        ) as LinearLayout
                    val mobileTypeSingle =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileTypeSingle"
                        ) as TextView
                    val mLight =
                        XposedHelpers.getObjectField(param?.thisObject, "mLight") as Boolean

                    setMobileType(mobileGroup, mobileTypeSingle, mobileLeftContainer, mLight)
                }
            })

        findAndHookMethod(
            statusBarMobileClass,
            "applyMobileState",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy\$MobileIconState",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val mobileLeftContainer =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileLeftContainer"
                        ) as ViewGroup
                    val mobileGroup =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileGroup"
                        ) as LinearLayout
                    val mobileTypeSingle =
                        XposedHelpers.getObjectField(
                            param?.thisObject,
                            "mMobileTypeSingle"
                        ) as TextView
                    val mLight =
                        XposedHelpers.getObjectField(param?.thisObject, "mLight") as Boolean

                    setMobileType(mobileGroup, mobileTypeSingle, mobileLeftContainer, mLight)
                }
            })
    }

    private fun getMobileViewForMIUI() {
        statusBarMobileClass.methodFinder()
            .filterByName("init")
            .single().createHook {
                after {
                    val mobileLeftContainer =
                        XposedHelpers.getObjectField(
                            it.thisObject,
                            "mMobileLeftContainer"
                        ) as ViewGroup
                    val mobileGroup =
                        XposedHelpers.getObjectField(it.thisObject, "mMobileGroup") as LinearLayout
                    val mobileTypeSingle =
                        XposedHelpers.getObjectField(it.thisObject, "mMobileTypeSingle") as TextView
                    val mLight = XposedHelpers.getObjectField(it.thisObject, "mLight") as Boolean

                    setMobileType(mobileGroup, mobileTypeSingle, mobileLeftContainer, mLight)
                }
            }
    }

    @SuppressLint("SetTextI18n")
    private fun setMobileType(
        mobileGroup: LinearLayout,
        mobileTypeSingle: TextView,
        mobileLeftContainer: ViewGroup,
        mLight: Boolean
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

        if (isShowPaw) {
            val originalDrawable = mobileTypeSingle.context.resources.getDrawable(
                if (mLight) {
                    R.drawable.ic_paw
                } else {
                    R.drawable.ic_paw_dark
                }
            )
            val transparentDrawableLeft = ColorDrawable(Color.TRANSPARENT)
            val transparentDrawableRight = ColorDrawable(Color.TRANSPARENT)

            val additionalPaddingLeft = dp2px(1.2f)
            var additionalPaddingRight = dp2px(4.6f)

            if ((qpt == 0 && (!dataConnected || wifiAvailable)) || (qpt == 2 && wifiAvailable) || (qpt == 4 && !dataConnected && wifiAvailable)) {
                additionalPaddingRight = dp2px(1.6f)
            }

            // Set bounds for the transparent drawables
            transparentDrawableLeft.setBounds(0, 0, additionalPaddingLeft, originalDrawable.intrinsicHeight)
            transparentDrawableRight.setBounds(0, 0, additionalPaddingRight, originalDrawable.intrinsicHeight)

            // Create a LayerDrawable with the original drawable and transparent drawables
            val layers = arrayOf<Drawable>(transparentDrawableLeft, originalDrawable, transparentDrawableRight)
            val layerDrawable = LayerDrawable(layers)

            // Set the padding for the original drawable inside the LayerDrawable
            layerDrawable.setLayerInset(1, additionalPaddingLeft, 0, additionalPaddingRight, 0)

            mobileTypeSingle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                layerDrawable,
                null,
                null,
                null
            )
        }


        mobileTypeSingle.setPaddingRelative(marginLeft, topMargin, marginRight, 0)
    }
}
