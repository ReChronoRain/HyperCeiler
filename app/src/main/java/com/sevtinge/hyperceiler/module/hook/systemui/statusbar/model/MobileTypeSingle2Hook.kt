/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.annotation.*
import android.graphics.*
import android.telephony.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.hdController
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.mOperatorConfig
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.miuiCellularIconVM
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.shadeCarrierGroupController
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.bold
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.fontSize
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.getLocation
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.leftMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.mobileNetworkType
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.rightMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.showMobileType
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.verticalOffset
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import java.util.function.*

object MobileTypeSingle2Hook : BaseHook() {
    private val mobileSignalViewMap = HashMap<Int, MutableSet<View>>()

    override fun init() {
        // by customiuizer
        hookAllConstructors(miuiCellularIconVM, object : MethodHook() {
            override fun after(param: MethodHookParam) {
                // 显示逻辑
                hookMobileView(param.thisObject)
            }
        })

        if (showMobileType && mobileNetworkType == 4) {
            showMobileTypeSingle()
        }
    }

    private fun hookMobileView(cellularIcon: Any) {
        modernStatusBarMobileView.methodFinder()
            .filterByName("constructAndBind")
            .single().createHook {
                after { param ->
                    val rootView = param.result as ViewGroup
                    val subId = rootView.getIntField("subId")

                    val mobileGroup =
                        rootView.findViewByIdName("mobile_group") as LinearLayout
                    val mobileGroupParent = mobileGroup.parent as ViewGroup
                    val containerLeft =
                        mobileGroup.findViewByIdName("mobile_container_left") as ViewGroup
                    val containerRight =
                        mobileGroup.findViewByIdName("mobile_container_right") as ViewGroup
                    val mobileType = containerLeft.findViewByIdName("mobile_type") as? ImageView?

                    if (mobileSignalViewMap[subId] == null) {
                        mobileSignalViewMap[subId] = mutableSetOf()
                    }
                    mobileSignalViewMap[subId]?.add(mobileGroupParent)

                    // 添加大 5G 并设置样式
                    if (showMobileType) {
                        val textView =
                            mobileGroup.findViewByIdName("mobile_type_single") as TextView
                        if (!getLocation) {
                            mobileGroup.removeView(textView)
                            mobileGroup.addView(textView)
                        }
                        if (fontSize != 27) {
                            textView.textSize = fontSize * 0.5f
                        }
                        if (bold) {
                            textView.typeface = Typeface.DEFAULT_BOLD
                        }
                        val marginLeft = dp2px(leftMargin * 0.5f)
                        val marginRight = dp2px(rightMargin * 0.5f)
                        var topMargin = 0
                        if (verticalOffset != 8) {
                            val marginTop = dp2px((verticalOffset - 8) * 0.5f)
                            topMargin = marginTop
                        }
                        textView.setPadding(marginLeft, topMargin, marginRight, 0)

                        // 大 5G 始终删除小 5G
                        containerLeft.removeView(mobileType)
                    }

                    // 调整初始样式
                    if (mobileNetworkType == 3 || mobileNetworkType == 4 || showMobileType) {
                        containerLeft.setPadding(20, 0, 0, 0)
                        containerRight.setPadding(20, 0, 0, 0)
                    }
                }
            }

        shadeCarrierGroupController.methodFinder()
            .filterByName("updateModernMobileIcons")
            .single().createHook {
                after { param ->
                    val subList = param.args[0] as List<*>
                    if (subList.isEmpty()) {
                        return@after
                    }

                    if ((!hideIndicator && (showMobileType || mobileNetworkType == 3)) || mobileNetworkType == 4) {
                        dataChangedMobileType()
                    }

                    if (showMobileType && mobileNetworkType != 4) {
                        // 大 5G 显示逻辑
                        cellularIcon.setObjectField(
                            "mobileTypeSingleVisible",
                            newReadonlyStateFlow(true)
                        )
                        if (mobileNetworkType == 0 || mobileNetworkType == 2) {
                            showWifi()
                        }
                    } else if (!showMobileType) {
                        // 小 5G 显示逻辑
                        if (mobileNetworkType == 1) {
                            cellularIcon.setObjectField(
                                "mobileTypeVisible",
                                newReadonlyStateFlow(true)
                            )
                        } else if (mobileNetworkType == 3 || mobileNetworkType == 4) {
                            cellularIcon.setObjectField(
                                "mobileTypeVisible",
                                newReadonlyStateFlow(false)
                            )
                        }
                    }
                }
            }
    }

    private fun showMobileTypeSingle() {
        mOperatorConfig.constructors[0].createHook {
            after {
                // 启用系统的网络类型单独显示
                // 系统的单独显示只有一个大 5G
                it.thisObject.setObjectField("showMobileDataTypeSingle", true)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun dataChangedMobileType() {
        val javaAdapter = Dependency.mMiuiLegacyDependency
            ?.getObjectField("mCentralSurfaces")
            ?.callMethod("get")
            ?.getObjectField("mJavaAdapter")

        val dataConnected = Dependency.mMiuiLegacyDependency
            ?.getObjectField("mMiuiIconManagerFactory")
            ?.callMethod("get")
            ?.getObjectField("mMobileUiAdapter")
            ?.getObjectField("hdController")
            ?.getObjectField("mMiuiMobileIconsInt")
            ?.getObjectField("dataConnected")

        // 监听移动网络
        javaAdapter?.callMethod(
            "alwaysCollectFlow",
            dataConnected,
            Consumer<BooleanArray> {
                val simCount = it.size

                val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                mobileSignalViewMap[subId]?.forEach { view ->
                    val containerLeft = view.findViewByIdName("mobile_container_left") as ViewGroup
                    val containerRight =
                        view.findViewByIdName("mobile_container_right") as ViewGroup

                    val isNoDataConnected = when (simCount) {
                        1 -> !it[0]
                        2 -> !it[0] && !it[1]
                        else -> false
                    }
                    if (!showMobileType && mobileNetworkType == 4) {
                        val mobileType = view.findViewByIdName("mobile_type") as ImageView
                        mobileType.visibility = if (isNoDataConnected || showMobileType) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                    }
                    val paddingLeft = if (isNoDataConnected || (showMobileType && hideIndicator)) {
                        20
                    } else {
                        0
                    }
                    containerLeft.setPadding(paddingLeft, 0, 0, 0)
                    containerRight.setPadding(paddingLeft, 0, 0, 0)
                }
                if (mobileSignalViewMap.size == simCount) {
                    mobileSignalViewMap[(mobileSignalViewMap.keys - subId).single()]?.forEach { view ->
                        val containerLeft =
                            view.findViewByIdName("mobile_container_left") as ViewGroup
                        val containerRight =
                            view.findViewByIdName("mobile_container_right") as ViewGroup
                        if (!showMobileType && mobileNetworkType == 4) {
                            val mobileType = view.findViewByIdName("mobile_type") as ImageView
                            mobileType.visibility = View.GONE
                        }
                        containerLeft.setPadding(20, 0, 0, 0)
                        containerRight.setPadding(20, 0, 0, 0)
                    }
                }
            }
        )
    }

    private fun setViewVisibility(getId: String, visibility: Int) {
        mobileSignalViewMap.forEach { (_, v) ->
            v.forEach {
                val mMobileType = it.findViewByIdName(getId) as View
                mMobileType.visibility = visibility
            }
        }
    }

    private fun showWifi() {
        hdController.methodFinder().filterByName("update")
            .single().createHook {
                after {
                    val mWifiAvailable = it.thisObject.getBooleanField("mWifiAvailable")

                    if (mWifiAvailable) {
                        setViewVisibility("mobile_type_single", View.GONE)
                    } else {
                        setViewVisibility("mobile_type_single", View.VISIBLE)
                    }
                }
            }
    }
}