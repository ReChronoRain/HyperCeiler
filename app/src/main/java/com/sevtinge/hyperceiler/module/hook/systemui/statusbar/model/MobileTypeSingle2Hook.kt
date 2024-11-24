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

import android.graphics.*
import android.telephony.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.DualRowSignalHookV.Companion.STATUS_BAR_MOBILE_VIEW
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.hdController
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.mOperatorConfig
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.miuiCellularIconVM
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.miuiMobileIconBinder
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.miuiCarrier
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.bold
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.card1
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.card2
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.fontSize
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.getLocation
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.hideRoaming
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.leftMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.mobileNetworkType
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.rightMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.showMobileType
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.verticalOffset
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import java.lang.reflect.*
import java.util.ArrayList

object MobileTypeSingle2Hook : BaseHook() {
    private val DarkIconDispatcherClass: Class<*> by lazy {
        loadClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
    }
    var method: Method? = null
    var method2: Method? = null
    private var mobileId = -1
    private var get0: Float = 0.0f
    private var get1: Int = 0
    private var get2: Int = 0
    private val mobileSignalViewMap = HashMap<Int, MutableList<View>>()

    override fun init() {
        // by customiuizer
        hookAllConstructors(miuiCellularIconVM,
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    val cellularIcon = param.thisObject
                    if (hideIndicator) {
                        cellularIcon.setObjectField("inOutVisible", newReadonlyStateFlow(false))
                    }
                    if (hideRoaming) {
                        cellularIcon.setObjectField("smallRoamVisible", newReadonlyStateFlow(false))
                        cellularIcon.setObjectField(
                            "mobileRoamVisible",
                            newReadonlyStateFlow(false)
                        )
                    }
                    // 隐藏 hd
                    updateIconState(param, "smallHdVisible", "system_ui_status_bar_icon_small_hd")
                    updateIconState(param, "volteVisibleCn", "system_ui_status_bar_icon_big_hd")
                    updateIconState(param, "volteVisibleGlobal", "system_ui_status_bar_icon_big_hd")
                    // 显示逻辑
                    hookMobileView(cellularIcon)
                    setMobileType(cellularIcon)
                }
            }
        )
        if (!showMobileType) return
        if (mobileNetworkType == 4) showMobileTypeSingle()
        miuiMobileIconBinder.methodFinder().filterByName("bind").single()
            .createHook {
                after {
                    // 获取布局
                    val getView = it.args[0] as ViewGroup
                    if ("mobile" == getView.getObjectFieldAs<String>("slot")) {
                        // 大 5G 的 View
                        val textView: TextView =
                            getView.findViewById(
                                getView.resources.getIdentifier("mobile_type_single", "id", "com.android.systemui")
                            )
                        val layout = textView.parent as LinearLayout
                        val getView2: ViewGroup =
                            getView.findViewById(
                                getView.resources.getIdentifier("mobile_container_left", "id", "com.android.systemui")
                            )

                        if (!getLocation) {
                            layout.removeView(textView)
                            layout.addView(textView)
                        }
                        if (fontSize != 27) {
                            textView.textSize = fontSize * 0.5f
                        }
                        if (bold) {
                            textView.typeface = Typeface.DEFAULT_BOLD
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
                        textView.setPadding(marginLeft, topMargin, marginRight, 0)

                        // 整理布局，删除多余元素
                        layout.removeView(getView2)
                        val layout2 = FrameLayout(getView.context)
                        layout.addView(layout2)
                        layout2.visibility = View.GONE
                        layout2.addView(getView2)
                    }
                }
            }

        try {
            method = DarkIconDispatcherClass.getMethod("isInAreas", MutableCollection::class.java, View::class.java)
            try {
                method2 = DarkIconDispatcherClass.getMethod("getTint", MutableCollection::class.java, View::class.java, Integer.TYPE)
            } catch (unused: Throwable) {
                logE(TAG, lpparam.packageName, "DarkIconDispatcher.isInArea not found")
                if (method != null) {
                    return
                }
                return
            }
        } catch (unused2: Throwable) {
            method = null
        }
        if (method == null || method2 == null) {
            return
        }

        findAndHookMethod("com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView", "onDarkChanged", ArrayList::class.java, Float::class.java, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean::class.java, object : MethodHook() {
            override fun after(it: MethodHookParam) {
                if ("mobile" == it.thisObject.getObjectFieldAs<String>("slot")) {
                    get0 =  it.args[1] as Float
                    get1 = it.args[3] as Int
                    get2 = it.args[4] as Int
                    val num = it.args[2] as Int
                    val getBoolean = it.args[5] as Boolean
                    val getView = it.thisObject as ViewGroup
                    if (mobileId < 1) {
                        mobileId = getView.resources.getIdentifier("mobile_type_single", "id", "com.android.systemui")
                    }
                    val textView: TextView = getView.findViewById(mobileId)
                    if (getBoolean) {
                        method2?.invoke(null, it.args[0], textView, num)?.let { it1 ->
                            textView.setTextColor(it1.hashCode())
                        }
                        return
                    }
                    val getBoolean2 = method?.invoke(null, it.args[0], textView)?.let { it1 ->
                        textView.setTextColor(num)
                    } as Boolean
                    if (getBoolean2) {
                        get0 = 0.0f
                    }
                    if (get0 > 0.0f) {
                        get1 = get2
                    }
                    textView.setTextColor(get1)
                    return
                }
            }
        })
    }

    private fun updateIconState(param: MethodHookParam, fieldName: String, key: String) {
        val opt = mPrefsMap.getStringAsInt(key, 0)
        if (opt != 0) {
            val value = when (opt) {
                1 -> true
                else -> false
            }
            param.thisObject.setObjectField(fieldName, newReadonlyStateFlow(value))
        }
    }

    private fun hookMobileView(cellularIcon: Any) {
        loadClass(STATUS_BAR_MOBILE_VIEW).methodFinder()
            .filterByName("constructAndBind")
            .single().createHook {
                after { param ->
                    val rootView = param.result as ViewGroup
                    val mobileGroup =
                        rootView.findViewByIdName("mobile_group") as LinearLayout
                    val mobileGroupParent = mobileGroup.parent as ViewGroup
                    val subId = rootView.getIntField("subId")
                    val getSlotIndex = SubscriptionManager.getSlotIndex(subId)

                    if (mobileSignalViewMap[subId] == null) {
                        mobileSignalViewMap[subId] = mutableListOf()
                    }
                    mobileSignalViewMap[subId]?.add(mobileGroupParent)

                    if (showMobileType && (mobileNetworkType == 0 || mobileNetworkType == 2)) {
                        cellularIcon.setObjectField("mobileTypeSingleVisible", newReadonlyStateFlow(true))
                        showWifi(mobileGroupParent)
                    } /*else if (!showMobileType && mobileNetworkType == 4) {
                        smallMobileType(mobileGroupParent, subId)
                    }*/

                    // 隐藏 Sim 卡图标
                    mobileSignalViewMap[subId]?.forEach {
                        it.post {
                            if ((card1 && getSlotIndex == 0) || (card2 && getSlotIndex == 1)) {
                                it.visibility = View.GONE
                            }
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

    private fun smallMobileType(mobileGroupParent: ViewGroup, subId: Int) {
        val getSlotIndex = SubscriptionManager.getSlotIndex(subId)
        miuiCarrier.methodFinder().filterByName("updateCarrierIfNeed")
            .single().createHook {
                after {
                    val dataConnected = it.thisObject.getObjectFieldAs<BooleanArray>("mDataConnected")
                    val get = it.args[0] as Int
                    // 需要解决获取 dataConnected 的问题
                    /*if (getSlotIndex == 0 && !dataConnected[0]) {
                        setView(mobileGroupParent, "mobile_type", View.GONE)
                    } else if (getSlotIndex == 0 && dataConnected[0]) {
                        setView(mobileGroupParent, "mobile_type", View.VISIBLE)
                    }

                    if (getSlotIndex == 1 && !dataConnected[0]) {
                        setView(mobileGroupParent, "mobile_type", View.GONE)
                    } else if (getSlotIndex == 1 && dataConnected[0]) {
                        setView(mobileGroupParent, "mobile_type", View.VISIBLE)
                    }*/
                }
            }
    }


    private fun setView(its: View, name: String, visibility: Int) {
        val mMobileType =
            its.findViewByIdName(name) as View
        mMobileType.visibility = visibility
    }

    private fun showWifi(mobileGroupParent: ViewGroup) {
        hdController.methodFinder().filterByName("update")
            .single().createHook {
                after {
                    val mWifiAvailable =
                        it.thisObject.getObjectFieldAs<Boolean>("mWifiAvailable")

                   if (mWifiAvailable) {
                       setView(mobileGroupParent, "mobile_type_single", View.GONE)
                   } else {
                       setView(mobileGroupParent, "mobile_type_single", View.VISIBLE)
                   }
                }
            }
    }

    private fun setMobileType(cellularIcon: Any) {
        if (showMobileType) {
            // 大 5G 显示逻辑
            if (mobileNetworkType == 1) {
                cellularIcon.setObjectField("mobileTypeSingleVisible", newReadonlyStateFlow(true))
            }
        } else {
            // 小 5G 显示逻辑
            when (mobileNetworkType) {
                1 -> {
                    cellularIcon.setObjectField("mobileTypeVisible", newReadonlyStateFlow(true))
                }
                3 -> {
                    // 需要解决信号图标错位问题
                    cellularIcon.setObjectField("mobileTypeVisible", newReadonlyStateFlow(false))
                }
                // 0 和 2 保持一致
            }
        }
    }
}