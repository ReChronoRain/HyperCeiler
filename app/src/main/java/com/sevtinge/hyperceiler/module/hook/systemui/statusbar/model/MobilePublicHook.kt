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

import android.telephony.SubscriptionManager
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.card1
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.card2
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.getIntField
import com.sevtinge.hyperceiler.utils.setObjectField
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers

object MobilePublicHook : BaseHook() {
    private val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }

    private val isEnableDouble by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_icon_enable")
    }
    private val qpt by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0)
    }
    private val hideIndicator by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator")
    }
    private val singleMobileType by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable")
    }
    private val isHideRoaming by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_mobile_hide_roaming_icon")
    }

    override fun init() {
        updateState()
        applyMobileState()
    }

    private fun updateState() {
        hookAllMethods(statusBarMobileClass, "updateState", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (isHyperOSVersion(1f)) {
                    hideSimCard(param)
                }
            }

            override fun after(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator) {
                    hideMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
                hideIcons(param)
            }
        })
    }

    private fun applyMobileState() {
        hookAllMethods(statusBarMobileClass, "applyMobileState", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (singleMobileType) {
                    showMobileTypeSingle(param) // 使网络类型单独显示
                }
                if (isHyperOSVersion(1f)) {
                    hideSimCard(param)
                }
            }

            override fun after(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator) {
                    hideMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
                hideIcons(param)
            }
        })
    }

    private fun hideSimCard(param: MethodHookParam) {
        val getSim = param.args[0]
        val getSubId = getSim.getIntField("subId")
        val getSlotIndex = SubscriptionManager.getSlotIndex(getSubId)

        if ((card1 && getSlotIndex == 0) || (card2 && getSlotIndex == 1)) {
            getSim.setObjectField("visible", false)
        }
    }

    private fun showMobileTypeSingle(param: MethodHookParam) {
        // 使网络类型单独显示
        try {
            val mobileIconState = param.args[0]
            mobileIconState.setObjectField("showMobileDataTypeSingle", true)
            mobileIconState.setObjectField("fiveGDrawableId", 0)
        } catch (t: Throwable) {
            logE(TAG, "showMobileTypeSingle setObjectField is null, $t")
        }
    }

    private fun hideIcons(param: MethodHookParam) {
        val mSmallRoaming = XposedHelpers.getObjectField(param.thisObject, "mSmallRoaming") as View
        val mMobileRoaming = XposedHelpers.getObjectField(param.thisObject, "mMobileRoaming") as View

        if (isHideRoaming) {
            if (!isEnableDouble) {
                mSmallRoaming.visibility = View.GONE
            }
            mMobileRoaming.visibility = View.GONE
        }
    }

    private fun hideMobileType(param: MethodHookParam) {
        // 隐藏网络类型图标
        val mMobileType: View = getMobileType(param)
        // 隐藏移动网络活动指示器
        val mLeftInOut = XposedHelpers.getObjectField(param.thisObject, "mLeftInOut") as View
        if (hideIndicator) {
            val mRightInOut = XposedHelpers.getObjectField(param.thisObject, "mRightInOut") as View
            mLeftInOut.visibility = View.GONE
            mRightInOut.visibility = View.GONE
        }
        if (mMobileType.visibility == View.GONE && mLeftInOut.visibility == View.GONE) {
            val mMobileLeftContainer =
                XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer") as View
            mMobileLeftContainer.visibility = View.GONE
        }
    }

    private fun getMobileType(param: MethodHookParam): View {
        val mMobileType = XposedHelpers.getObjectField(param.thisObject, "mMobileType") as View
        val dataConnected = XposedHelpers.getObjectField(param.args[0], "dataConnected") as Boolean
        val wifiAvailable = XposedHelpers.getObjectField(param.args[0], "wifiAvailable") as Boolean

        if (qpt != 0) {
            val mMobileTypeSingle =
                if (singleMobileType)
                    XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle") as TextView
                else null
            val visibility =
                if (qpt == 1 || (qpt == 2 && !wifiAvailable) || (qpt == 4 && dataConnected && !wifiAvailable)) View.VISIBLE else View.GONE

            if (singleMobileType && mMobileTypeSingle != null) {
                mMobileTypeSingle.visibility = visibility
            } else {
                mMobileType.visibility = visibility
            }
        }
        return mMobileType
    }
}
