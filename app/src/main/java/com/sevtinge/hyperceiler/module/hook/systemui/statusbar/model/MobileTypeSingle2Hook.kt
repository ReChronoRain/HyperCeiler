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
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.hook.systemui.*
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.hdController
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.mOperatorConfig
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobileClass.shadeHeaderController
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.bold
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.fontSize
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.getLocation
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.isEnableDouble
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.leftMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.mobileNetworkType
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.rightMargin
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.showMobileType
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public.MobilePrefs.verticalOffset
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.utils.api.ProjectApi.*
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import java.lang.reflect.*
import java.util.function.*

object MobileTypeSingle2Hook : BaseHook() {
    private val DarkIconDispatcherClass by lazy {
        loadClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
    }
    var method: Method? = null
    var method2: Method? = null
    private var get0: Float = 0.0f
    private var get1: Int = 0
    private var get2: Int = 0
    private val simDataConnected = booleanArrayOf(false, false)

    override fun init() {
        if (mobileNetworkType != 0) {
            hookMobileView()
        }

        if (!showMobileType) {
            return
        }

        try {
            method = DarkIconDispatcherClass.getMethod(
                "isInAreas",
                MutableCollection::class.java,
                View::class.java
            )

            try {
                method2 = DarkIconDispatcherClass.getMethod(
                    "getTint",
                    MutableCollection::class.java,
                    View::class.java,
                    Integer.TYPE
                )
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

        findAndHookMethod(
            "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView",
            "onDarkChanged",
            ArrayList::class.java,
            Float::class.java,
            Integer.TYPE,
            Integer.TYPE,
            Integer.TYPE,
            Boolean::class.java,
            object : MethodHook() {
                override fun after(it: MethodHookParam) {
                    if ("mobile" == it.thisObject.getObjectFieldAs<String>("slot")) {
                        get0 = it.args[1] as Float
                        get1 = it.args[3] as Int
                        get2 = it.args[4] as Int
                        val num = it.args[2] as Int
                        val getBoolean = it.args[5] as Boolean
                        val getView = it.thisObject as ViewGroup
                        val textView: TextView =
                            getView.findViewByIdName("mobile_type_single") as TextView

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

    private fun hookMobileView() {
        modernStatusBarMobileView.methodFinder()
            .filterByName("constructAndBind")
            .filterByParamCount(5)
            .single().createHook {
                after { param ->
                    val viewModel = param.args[4]
                    val rootView = param.result as ViewGroup
                    val subId = rootView.getIntField("subId")

                    val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout
                    val containerLeft =
                        mobileGroup.findViewByIdName("mobile_container_left") as ViewGroup
                    val containerRight =
                        mobileGroup.findViewByIdName("mobile_container_right") as ViewGroup

                    // 添加大 5G 并设置样式
                    if (showMobileType) {
                        val mobileType = containerLeft.findViewByIdName("mobile_type") as? ImageView?
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
                    val dataConnected = simDataConnected[SubscriptionManager.getSlotIndex(subId)]
                    if (mobileNetworkType == 3 || mobileNetworkType == 4 || showMobileType) {
                        val paddingLeft = if (dataConnected && !(showMobileType && hideIndicator)) {
                            0
                        } else {
                            20
                        }

                        containerLeft.setPadding(paddingLeft, 0, 0, 0)
                        containerRight.setPadding(paddingLeft, 0, 0, 0)
                    }

                    if (showMobileType && mobileNetworkType != 4) {
                        // 大 5G 显示逻辑
                        viewModel.setObjectField(
                            "mobileTypeSingleVisible",
                            newReadonlyStateFlow(true)
                        )
                    } else if (!showMobileType) {
                        // 小 5G 显示逻辑
                        viewModel.setObjectField(
                            "mobileTypeVisible",
                            newReadonlyStateFlow(
                                when (mobileNetworkType) {
                                    1, 2 -> true
                                    3 -> false
                                    else -> dataConnected
                                }
                            )
                        )
                    }
                }
            }

        shadeHeaderController.methodFinder()
            .filterByName("onViewAttached")
            .single()
            .createHook {
                after {
                    if ((!hideIndicator && (showMobileType || mobileNetworkType == 3)) || mobileNetworkType == 4) {
                        setOnDataChangedListener()
                    }
                }
            }

        if (showMobileType && mobileNetworkType == 4) {
            showMobileTypeSingle()
        }

        if (showMobileType && (mobileNetworkType == 0 || mobileNetworkType == 2)) {
            setOnWiFiChangedListener()
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
    private fun setOnDataChangedListener() {
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
                if (isDebug()) logD(TAG, lpparam.packageName, "MobileDataConnected -> ${it.contentToString()}")

                val simCount = it.size
                val isNoDataConnected = when (simCount) {
                    1 -> {
                        simDataConnected[0] = it[0]
                        !it[0]
                    }

                    2 -> {
                        simDataConnected[0] = it[0]
                        simDataConnected[1] = it[1]
                        !it[0] && !it[1]
                    }

                    else -> {
                        simDataConnected[0] = false
                        simDataConnected[1] = false
                        false
                    }
                }
                SubscriptionManagerProvider(EzXHelper.appContext).getActiveSubscriptionIdList(true)
                    .forEach { subId ->
                        getMobileViewBySubId(subId) { view ->
                            val containerLeft =
                                view.findViewByIdName("mobile_container_left") as ViewGroup
                            val containerRight =
                                view.findViewByIdName("mobile_container_right") as ViewGroup

                            val b = (!showMobileType && mobileNetworkType == 4)
                            if (subId == SubscriptionManager.getDefaultDataSubscriptionId()) {
                                if (b) {
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
                            } else if (!isEnableDouble) {
                                if (b) {
                                    val mobileType = view.findViewByIdName("mobile_type") as ImageView
                                    mobileType.visibility = View.GONE
                                }
                                containerLeft.setPadding(20, 0, 0, 0)
                                containerRight.setPadding(20, 0, 0, 0)
                            }
                        }
                    }
            }
        )
    }

    private fun setOnWiFiChangedListener() {
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

    private fun setViewVisibility(getId: String, visibility: Int) {
        SubscriptionManagerProvider(EzXHelper.appContext).getActiveSubscriptionIdList(true)
            .forEach { subId ->
                getMobileViewBySubId(subId) { view: View ->
                    val mMobileType = view.findViewByIdName(getId) as View
                    mMobileType.visibility = visibility
                }
            }
    }

    private fun getMobileViewBySubId(subId: Int, callback: (View) -> Unit) {
        val statusBarIconController = Dependency.mMiuiLegacyDependency
            ?.getObjectField("mStatusBarIconController")
            ?.callMethod("get")

        if (statusBarIconController == null) {
            return
        }

        val iconGroups = statusBarIconController.getObjectFieldAs<Map<Any, *>>("mIconGroups")
        val iconList = statusBarIconController.getObjectFieldAs<Any>("mStatusBarIconList")

        val viewIndex = iconList.callMethodAs<Int>("getViewIndex", subId, "mobile")

        iconGroups.forEach { (iconManager, _) ->
            val child = iconManager.getObjectFieldAs<ViewGroup>("mGroup").getChildAt(viewIndex)

            if (child is View &&
                "ModernStatusBarMobileView" == child::class.java.simpleName &&
                "mobile" == child.getObjectField("slot")
            ) {
                callback(child)
            }
        }
    }
}