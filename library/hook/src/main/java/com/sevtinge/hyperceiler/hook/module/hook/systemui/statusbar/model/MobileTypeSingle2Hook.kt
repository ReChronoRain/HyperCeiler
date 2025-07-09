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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.model

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.Dependency
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.MiuiStub
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobileClass.mOperatorConfig
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobileClass.miuiCellularIconVM
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobileClass.mobileUiAdapter
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.bold
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.fontSize
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.getLocation
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.isEnableDouble
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.leftMargin
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.mobileNetworkType
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.rightMargin
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.showMobileType
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.statusbar.icon.MobilePrefs.verticalOffset
import com.sevtinge.hyperceiler.hook.utils.StateFlowHelper.getStateFlowValue
import com.sevtinge.hyperceiler.hook.utils.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.hook.utils.StateFlowHelper.setStateFlowValue
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi.isDebug
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callMethodAs
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.devicesdk.SubscriptionManagerProvider
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreSmallVersion
import com.sevtinge.hyperceiler.hook.utils.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.hook.utils.getBooleanField
import com.sevtinge.hyperceiler.hook.utils.getIntField
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.setAdditionalInstanceField
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import io.github.kyuubiran.ezxhelper.android.util.ViewUtil.findViewByIdName
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import java.lang.reflect.Method
import java.util.function.Consumer

object MobileTypeSingle2Hook : BaseHook() {
    private val isMore200SmallVersion = isMoreSmallVersion(200, 2f)

    private val DarkIconDispatcherClass by lazy {
        loadClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
    }
    var method: Method? = null
    var method2: Method? = null
    private var get0: Float = 0.0f
    private var get1: Int = 0
    private var get2: Int = 0

    override fun init() {
        hookMobileViewAndVM()
        if (!showMobileType) return

        try {
            method = DarkIconDispatcherClass.getMethod(
                "isInAreas",
                MutableCollection::class.java,
                View::class.java
            )
            method2 = DarkIconDispatcherClass.getMethod(
                "getTint",
                MutableCollection::class.java,
                View::class.java,
                Integer.TYPE
            )
        } catch (_: Throwable) {
            logE(TAG, lpparam.packageName, "DarkIconDispatcher methods not found")
            method = null
            method2 = null
        }
        if (method == null && method2 == null) {
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

    private fun hookMobileViewAndVM() {

        if (mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_type_compatibility")) {
            if (mobileNetworkType == 3 || mobileNetworkType == 4 || showMobileType) {
                if (isMoreSmallVersion(200, 2f)) {
                    findAndHookMethod("com.android.systemui.statusbar.views.MobileTypeDrawable", "measure", object : MethodHook() {
                        override fun before(param: MethodHookParam?) {
                            param!!.result = null
                        }
                    })
                }
            }
        }

        miuiCellularIconVM.constructorFinder().first().createAfterHook { param ->
            val viewModel = param.thisObject
            viewModel.setAdditionalInstanceField("interactor", param.args[1])
            viewModel.setObjectField("wifiAvailable", param.args[2].getObjectField("wifiAvailable"))
        }

        modernStatusBarMobileView.methodFinder()
            .filterByName("constructAndBind")
            .filterByParamCount(5)
            .single().createAfterHook { param ->
                var viewModel = param.args.last()
                if (viewModel.javaClass.simpleName == "MiuiMobileIconVMImpl") {
                    viewModel = viewModel.callMethodAs("getCellProvider")
                }
                val interactor = viewModel.getAdditionalInstanceFieldAs<Any>("interactor")

                val rootView = param.result as ViewGroup
                val subId = rootView.getIntField("subId")

                val slotIndex = SubscriptionManager.getSlotIndex(subId)
                if (slotIndex == -1) {
                    return@createAfterHook
                }

                val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout
                val containerLeft =
                    mobileGroup.findViewByIdName("mobile_container_left") as ViewGroup
                val containerRight =
                    mobileGroup.findViewByIdName("mobile_container_right") as ViewGroup

                // 添加大 5G 并设置样式
                if (showMobileType) {
                    val mobileType = containerLeft.findViewByIdName("mobile_type") as? ImageView
                    val textView = mobileGroup.findViewByIdName("mobile_type_single") as TextView
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
                    if (verticalOffset != 40) {
                        val marginTop = dp2px((verticalOffset - 40) * 0.1f)
                        topMargin = marginTop
                    }
                    textView.setPadding(marginLeft, topMargin, marginRight, 0)

                    // 大 5G 始终删除小 5G
                    containerLeft.removeView(mobileType)
                }

                // 调整初始样式
                val isDataConnectedFlow = interactor.getObjectFieldAs<Any>("isDataConnected")
                // 在 200 版本 isDataConnected 初始总为 false
                val isDataConnected = getStateFlowValue(isDataConnectedFlow) as Boolean
                if (mobileNetworkType == 3 || mobileNetworkType == 4 || showMobileType) {
                    val paddingLeft = if (isDataConnected && !(showMobileType && hideIndicator)) {
                        0
                    } else {
                        20
                    }

                    containerLeft.setPadding(paddingLeft, 0, 0, 0)
                    containerRight.setPadding(paddingLeft, 0, 0, 0)
                }

                if (showMobileType) {
                    // 大 5G 显示逻辑
                    if (mobileNetworkType == 0 || mobileNetworkType == 2) {
                        viewModel.setObjectField(
                            "mobileTypeSingleVisible",
                            newReadonlyStateFlow(false)
                        )

                        MiuiStub.javaAdapter.alwaysCollectFlow(
                            viewModel.getObjectFieldAs("wifiAvailable"),
                            Consumer<Boolean> {
                                if (subId == SubscriptionManager.getDefaultDataSubscriptionId()) {
                                    val paddingLeft = if (it || hideIndicator) 20 else 0
                                    containerLeft.setPadding(paddingLeft, 0, 0, 0)
                                    containerRight.setPadding(paddingLeft, 0, 0, 0)
                                }

                                setStateFlowValue(
                                    viewModel.getObjectField("mobileTypeSingleVisible"), !it
                                )
                            }
                        )
                    } else if (mobileNetworkType != 4) {
                        viewModel.setObjectField(
                            "mobileTypeSingleVisible",
                            newReadonlyStateFlow(true)
                        )
                    }
                } else {
                    // 小 5G 显示逻辑
                    viewModel.setObjectField(
                        "mobileTypeVisible",
                        when (mobileNetworkType) {
                            1, 2 -> newReadonlyStateFlow(true)
                            3 -> newReadonlyStateFlow(false)
                            else -> if (isMore200SmallVersion) {
                                isDataConnectedFlow
                            } else {
                                newReadonlyStateFlow(isDataConnected)
                            }
                        }
                    )
                }
            }

        if ((!hideIndicator && (showMobileType || mobileNetworkType == 3)) || mobileNetworkType == 4) {
            mobileUiAdapter.constructorFinder().first().createAfterHook {
                setOnDataChangedListener(it.thisObject)
            }
        }

        if (showMobileType && mobileNetworkType == 4) {
            showMobileTypeSingle()
        }
    }

    private fun showMobileTypeSingle() {
        mOperatorConfig.constructors[0].createAfterHook {
            // 启用系统的网络类型单独显示
            // 系统的单独显示只有一个大 5G
            it.thisObject.setObjectField("showMobileDataTypeSingle", true)
        }
    }

    @SuppressLint("NewApi")
    private fun setOnDataChangedListener(mobileUiAdapter: Any) {
        val miuiInt = mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
            .getObjectFieldAs<Any>("miuiInt")
        val defaultConnections = miuiInt.getObjectFieldAs<Any>("connectRepo")
            .getObjectFieldAs<Any>("defaultConnections")
        val dataConnected = miuiInt.getObjectFieldAs<Any>("dataConnected")

        // 监听移动网络
        MiuiStub.javaAdapter.alwaysCollectFlow(dataConnected, Consumer<BooleanArray> {
            if (isDebug()) {
                logD(TAG, lpparam.packageName, "MobileDataConnected -> ${it.contentToString()}")
            }

            val simCount = it.size
            val isNoDataConnected = when (simCount) {
                1 -> !it[0]
                2 -> !it[0] && !it[1]
                else -> false
            }
            SubscriptionManagerProvider(EzXposed.appContext).getActiveSubscriptionIdList(true)
                .forEach { subId ->
                    getMobileViewBySubId(subId) { view ->
                        val containerLeft =
                            view.findViewByIdName("mobile_container_left") as ViewGroup
                        val containerRight =
                            view.findViewByIdName("mobile_container_right") as ViewGroup

                        val b = !showMobileType && mobileNetworkType == 4
                        if (subId == SubscriptionManager.getDefaultDataSubscriptionId()) {
                            if (b) {
                                val mobileType = view.findViewByIdName("mobile_type") as ImageView
                                mobileType.isVisible = !(isNoDataConnected || showMobileType)
                            }

                            val defaultConnections = getStateFlowValue(defaultConnections)
                                ?.getObjectField("wifi")
                                ?.getBooleanField("isDefault")

                            val paddingLeft =
                                if (isNoDataConnected || (showMobileType && hideIndicator) ||
                                    (defaultConnections == true && !isEnableDouble &&
                                        !it[SubscriptionManager.getSlotIndex(subId)])
                                ) {
                                    20
                                } else {
                                    0
                                }
                            containerLeft.setPadding(paddingLeft, 0, 0, 0)
                            containerRight.setPadding(paddingLeft, 0, 0, 0)
                        } else if (!isEnableDouble) {
                            if (b) {
                                val mobileType = view.findViewByIdName("mobile_type") as ImageView
                                mobileType.isVisible = false
                            }
                            containerLeft.setPadding(20, 0, 0, 0)
                            containerRight.setPadding(0, 0, 0, 0)
                        }
                    }
                }
        })
    }

    private fun getMobileViewBySubId(subId: Int, callback: (View) -> Unit) {
        val statusBarIconController = Dependency.miuiLegacyDependency
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
