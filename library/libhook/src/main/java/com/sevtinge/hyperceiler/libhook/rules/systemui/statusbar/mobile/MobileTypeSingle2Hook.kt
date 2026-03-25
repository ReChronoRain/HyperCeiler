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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.view.isVisible
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.getStateFlowValue
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.newReadonlyStateFlow
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper.setStateFlowValue
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.DataSimFlowProxy
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.KotlinJob
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MiuiStub
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.mOperatorConfig
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.miuiCellularIconVM
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.miuiMobileIconBinder
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.mobileUiAdapter
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.modernStatusBarMobileView
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.bold
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.fontSize
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.getLocation
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.hideIndicator
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.isEnableDouble
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.leftMargin
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.mobileNetworkType
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.rightMargin
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.showMobileType
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobilePrefs.verticalOffset
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileViewHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.afterHookMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethodAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethodOrNull
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findViewByIdName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getAdditionalInstanceFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getBooleanField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIntField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setAdditionalInstanceField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.EzXposed
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createHook
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

object MobileTypeSingle2Hook : BaseHook() {
    private val isMoreOS3 by lazy { isMoreHyperOSVersion(3f) }

    private val darkIconDispatcherClass by lazy {
        loadClass("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
    }

    private var methodIsInAreas: Method? = null
    private var methodGetTint: Method? = null
    private var darkChangedAreaAlpha: Float = 0.0f
    private var darkChangedTint: Int = 0
    private var darkChangedLightTint: Int = 0

    private val showNameFlowProxy = DataSimFlowProxy("")
    private val mobileTypeSingleVisibleProxy = DataSimFlowProxy(false)
    private val mobileTypeVisibleProxy = DataSimFlowProxy(false)

    @Volatile
    private var broadcastRegistered = false

    @Volatile
    private var dataChangedCollectorSource: Any? = null

    @Volatile
    private var dataChangedCollectorJob: KotlinJob? = null

    @Volatile
    private var defaultConnectionsCollectorSource: Any? = null

    @Volatile
    private var defaultConnectionsCollectorJob: KotlinJob? = null

    @Volatile
    private var isWifiDefaultConnection: Boolean? = null

    @Volatile
    private var isAnyDataConnected: Boolean? = null

    private val boundViews = ConcurrentHashMap<Int, MutableSet<ViewGroup>>()
    private val subSlotIndices = ConcurrentHashMap<Int, Int>()
    private val showNameStates = ConcurrentHashMap<Int, String>()
    private val showNameReadyStates = ConcurrentHashMap<Int, Boolean>()
    private val inOutVisibleStates = ConcurrentHashMap<Int, Boolean>()
    private val dataConnectedStates = ConcurrentHashMap<Int, Boolean>()
    private val wifiAvailableStates = ConcurrentHashMap<Int, Boolean>()
    private val mobileTypeSingleVisibleStates = ConcurrentHashMap<Int, Boolean>()
    private val mobileTypeVisibleStates = ConcurrentHashMap<Int, Boolean>()

    @Volatile
    private var lastResolvedDataSubId: Int = -1

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val refreshBoundViewsRunnable = Runnable { refreshBoundViewsNow() }

    override fun init() {
        hookMobileViewAndVM()
        if (!showMobileType || isMoreOS3) return

        try {
            methodIsInAreas = darkIconDispatcherClass.getMethod(
                "isInAreas",
                MutableCollection::class.java,
                View::class.java
            )
            methodGetTint = darkIconDispatcherClass.getMethod(
                "getTint",
                MutableCollection::class.java,
                View::class.java,
                Integer.TYPE
            )
        } catch (_: Throwable) {
            XposedLog.w(TAG, lpparam.packageName, "DarkIconDispatcher methods not found")
            methodIsInAreas = null
            methodGetTint = null
        }
        if (methodIsInAreas == null && methodGetTint == null) return

        findClass("com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView")
            .afterHookMethod(
                "onDarkChanged",
                ArrayList::class.java,
                Float::class.java,
                Integer.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                Boolean::class.java
            ) { param ->
                if ("mobile" != param.thisObject.getObjectFieldAs<String>("slot")) return@afterHookMethod

                darkChangedAreaAlpha = param.args[1] as Float
                darkChangedTint = param.args[3] as Int
                darkChangedLightTint = param.args[4] as Int

                val textColor = param.args[2] as Int
                val useTint = param.args[5] as Boolean
                val root = param.thisObject as ViewGroup
                val textView = root.findViewByIdName("mobile_type_single") as TextView

                if (useTint) {
                    methodGetTint?.invoke(null, param.args[0], textView, textColor)?.let { tint ->
                        textView.setTextColor(tint.hashCode())
                    }
                    return@afterHookMethod
                }

                val inAreas = methodIsInAreas?.invoke(null, param.args[0], textView)?.let {
                    textView.setTextColor(textColor)
                } as Boolean
                if (inAreas) darkChangedAreaAlpha = 0.0f
                if (darkChangedAreaAlpha > 0.0f) darkChangedTint = darkChangedLightTint
                textView.setTextColor(darkChangedTint)
            }
    }

    @SuppressLint("MissingPermission")
    private fun hookMobileViewAndVM() {
        if (showMobileType || mobileNetworkType == 3) {
            if (isMoreAndroidVersion(36)) {
                loadClass("com.miui.systemui.statusbar.views.MobileTypeDrawable")
            } else {
                loadClass("com.android.systemui.statusbar.views.MobileTypeDrawable")
            }.methodFinder().filterByName("measure").first().createHook {
                returnConstant(null)
            }
        }

        miuiCellularIconVM.constructorFinder().first().createAfterHook { param ->
            val viewModel = param.thisObject
            val interactor = param.args[1]
            val miuiInteractor = param.args[2]

            viewModel.setAdditionalInstanceField("interactor", interactor)
            viewModel.setObjectField("wifiAvailable", miuiInteractor?.getObjectField("wifiAvailable"))

            if (!isEnableDouble) return@createAfterHook

            val subId = runCatching {
                miuiInteractor?.getObjectFieldAs<Int>("subId")
            }.getOrNull() ?: return@createAfterHook

            val slotIndex = SubscriptionManager.getSlotIndex(subId)
            viewModel.getObjectField("showName")?.let { originalFlow ->
                showNameFlowProxy.setupForSlot(slotIndex, subId, originalFlow, MobileViewHelper::isSingleSimMode)
                if (slotIndex == 0) {
                    viewModel.setObjectField("showName", showNameFlowProxy.proxy!!)
                }
            }
            viewModel.getObjectField("mobileTypeSingleVisible")?.let { originalFlow ->
                mobileTypeSingleVisibleProxy.setupForSlot(slotIndex, subId, originalFlow, MobileViewHelper::isSingleSimMode)
                if (slotIndex == 0) {
                    viewModel.setObjectField("mobileTypeSingleVisible", mobileTypeSingleVisibleProxy.proxy!!)
                }
            }
            viewModel.getObjectField("mobileTypeVisible")?.let { originalFlow ->
                mobileTypeVisibleProxy.setupForSlot(slotIndex, subId, originalFlow, MobileViewHelper::isSingleSimMode)
                if (slotIndex == 0) {
                    viewModel.setObjectField("mobileTypeVisible", mobileTypeVisibleProxy.proxy!!)
                }
            }

            registerMobileStateCollectors(
                viewModel,
                interactor,
                subId,
                slotIndex
            )

            registerDataSimBroadcast()
        }

        modernStatusBarMobileView.methodFinder()
            .filterByName("constructAndBind")
            .filterByParamCount(5)
            .single()
            .hook {
                val result = proceed()
                val rootView = result as? ViewGroup ?: return@hook result
                val viewModel = unwrapCellProviderViewModel(args.lastOrNull()) ?: return@hook result
                bindConstructedMobileView(rootView, viewModel)
                result
            }

        if (!showMobileType && mobileNetworkType == 4) {
            mobileUiAdapter.constructorFinder().first().createAfterHook {
                setOnDataChangedListener(it.thisObject)
            }
        }

        if (showMobileType && isEnableDouble && (mobileNetworkType == 0 || mobileNetworkType == 2)) {
            mobileUiAdapter.constructorFinder().first().createAfterHook {
                setOnDefaultConnectionsListener(it.thisObject)
            }
        }

        if (showMobileType && mobileNetworkType == 4) {
            showMobileTypeSingle()
        }
    }

    private fun showMobileTypeSingle() {
        mOperatorConfig.constructors[0].createAfterHook {
            it.thisObject.setObjectField("showMobileDataTypeSingle", true)
        }
    }

    private fun unwrapCellProviderViewModel(viewModel: Any?): Any? {
        if (viewModel == null) return null
        return if (viewModel.javaClass.simpleName == "MiuiMobileIconVMImpl") {
            viewModel.callMethodAs("getCellProvider")
        } else {
            viewModel
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun bindConstructedMobileView(rootView: ViewGroup, viewModel: Any) {
        val interactor = viewModel.getAdditionalInstanceFieldAs<Any>("interactor")
        val subId = rootView.getIntField("subId")
        val slotIndex = SubscriptionManager.getSlotIndex(subId)
        if (slotIndex == -1) return
        cacheBoundView(subId, rootView)

        val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout
        val containerLeft: ViewGroup
        var containerRight: ViewGroup? = null
        if (isMoreOS3) {
            containerLeft = mobileGroup.findViewByIdName("mobile_signal_container") as ViewGroup
        } else {
            containerLeft = mobileGroup.findViewByIdName("mobile_container_left") as ViewGroup
            containerRight = mobileGroup.findViewByIdName("mobile_container_right") as ViewGroup
        }

        // 大 5G 样式：移除小 5G ImageView，配置大 5G TextView
        if (showMobileType) {
            containerLeft.findViewByIdName("mobile_type")?.let { containerLeft.removeView(it) }
            val textView = mobileGroup.findViewByIdName("mobile_type_single") as TextView
            if (!getLocation) {
                mobileGroup.removeView(textView)
                mobileGroup.addView(textView)
            }
            if (fontSize != 27) textView.textSize = fontSize * 0.5f
            if (bold) textView.typeface = Typeface.DEFAULT_BOLD
            textView.setPadding(
                dp2px(leftMargin * 0.5f),
                if (verticalOffset != 40) dp2px((verticalOffset - 40) * 0.1f) else 0,
                dp2px(rightMargin * 0.5f),
                0
            )
        }

        if (showMobileType) {
            // ===== 大 5G 可见性 =====
            // 双排模式下 slot 1+ 整个视图已被 MobilePublicHookV 隐藏，跳过避免无效 Flow 订阅
            if (isEnableDouble && !MobileViewHelper.isSingleSimMode() && slotIndex != 0) {
                return
            }

            when (mobileNetworkType) {
                0, 2 -> {
                    if (isEnableDouble && !MobileViewHelper.isSingleSimMode()) {
                        // 双排模式在 applyBoundViewState 中按当前上网卡统一判定显隐，
                        // 避免 constructAndBind 之后再替换 Flow 导致文本与显隐不同步。
                    } else {
                        viewModel.setObjectField("mobileTypeSingleVisible", newReadonlyStateFlow(false))

                        val defaultConnections = if (isEnableDouble) {
                            runCatching {
                                interactor?.getObjectFieldAs<Any>("connectRepo")
                                    ?.getObjectFieldAs<Any>("defaultConnections")
                            }.getOrNull()
                        } else {
                            null
                        }

                        if (defaultConnections != null) {
                            bindMobileTypeSingleVisibilityWithDefaultConnections(viewModel, defaultConnections)
                        } else {
                            bindMobileTypeSingleVisibilityWithWifiFlow(
                                viewModel,
                                viewModel.getObjectFieldAs("wifiAvailable")
                            )
                        }
                    }
                }
                4 -> Unit
                else -> viewModel.setObjectField("mobileTypeSingleVisible", newReadonlyStateFlow(true))
            }
            return
        }

        // ===== 小 5G 可见性 =====
        // 双排的 slot 1+ 整个视图已被 MobilePublicHookV 隐藏
        if (isEnableDouble && !MobileViewHelper.isSingleSimMode() && slotIndex != 0) {
            return
        }

        when (mobileNetworkType) {
            0 -> Unit
            2 -> {
                // WiFi 可用时隐藏小 5G
                if (isEnableDouble) {
                    // 双排模式：用独立 Flow 替换，避免与系统 ViewModel 驱动的 Flow 竞争
                    val flow = newReadonlyStateFlow(true)
                    viewModel.setObjectField("mobileTypeVisible", flow)
                    MiuiStub.javaAdapter.alwaysCollectFlow(
                        viewModel.getObjectFieldAs("wifiAvailable"),
                        Consumer<Boolean> { wifiOn -> setStateFlowValue(flow, !wifiOn) }
                    )
                } else {
                    MiuiStub.javaAdapter.alwaysCollectFlow(
                        viewModel.getObjectFieldAs("wifiAvailable"),
                        Consumer<Boolean> { wifiOn ->
                            // OS2: 调整左右容器边距，补偿隐藏元素的间距
                            // OS3: ConstraintLayout 不需要 padding 补偿
                            if (!isMoreOS3 && subId == SubscriptionManager.getDefaultDataSubscriptionId()) {
                                val paddingLeft = if (wifiOn || hideIndicator) 20 else 0
                                containerLeft.setPadding(if (wifiOn) paddingLeft else 0, 0, 0, 0)
                                containerRight?.setPadding(paddingLeft, 0, 0, 0)
                            }
                            setStateFlowValue(viewModel.getObjectField("mobileTypeVisible"), !wifiOn)
                        }
                    )
                }
            }

            1 -> viewModel.setObjectField("mobileTypeVisible", newReadonlyStateFlow(true))
            3 -> viewModel.setObjectField("mobileTypeVisible", newReadonlyStateFlow(false))
            4 -> Unit
            else -> {
                val wifiFlow = viewModel.getObjectFieldAs<Any>("wifiAvailable")
                val dataConnectedFlow = interactor?.getObjectFieldAs<Any>("isDataConnected")

                // 先读当前值
                val initWifiOn = runCatching { getStateFlowValue(wifiFlow) as Boolean }.getOrDefault(false)
                val initDataConnected = runCatching { getStateFlowValue(dataConnectedFlow) as Boolean }.getOrDefault(false)

                val flow = newReadonlyStateFlow(!initWifiOn && initDataConnected)
                viewModel.setObjectField("mobileTypeVisible", flow)

                var wifiOn = initWifiOn
                var dataConnected = initDataConnected

                MiuiStub.javaAdapter.alwaysCollectFlow(
                    wifiFlow,
                    Consumer<Boolean> { on ->
                        wifiOn = on
                        setStateFlowValue(flow, !wifiOn && dataConnected)
                    }
                )

                dataConnectedFlow?.let {
                    MiuiStub.javaAdapter.alwaysCollectFlow(
                        it,
                        Consumer<Boolean> { connected ->
                            dataConnected = connected
                            setStateFlowValue(flow, !wifiOn && dataConnected)
                        }
                    )
                }
            }
        }

        applyBoundViewState(rootView)
    }

    private fun bindMobileTypeSingleVisibilityWithWifiFlow(viewModel: Any, wifiFlow: Any) {
        val visibleFlow = viewModel.getObjectField("mobileTypeSingleVisible")
        MiuiStub.javaAdapter.alwaysCollectFlow(
            wifiFlow,
            Consumer<Boolean> { wifiOn -> setStateFlowValue(visibleFlow, !wifiOn) }
        )
    }

    private fun bindMobileTypeSingleVisibilityWithDefaultConnections(viewModel: Any, defaultConnections: Any) {
        val visibleFlow = viewModel.getObjectField("mobileTypeSingleVisible")
        MiuiStub.javaAdapter.alwaysCollectFlow(
            defaultConnections,
            Consumer<Any> { conn ->
                val isWifiDefault = runCatching {
                    conn.getObjectField("wifi")?.getBooleanField("isDefault")
                }.getOrNull() == true
                setStateFlowValue(visibleFlow, !isWifiDefault)
            }
        )
    }

    /** 监听上网卡切换 + SIM 变化，刷新已绑定的官方 mobile 布局 */
    @Synchronized
    private fun registerDataSimBroadcast() {
        if (broadcastRegistered) return
        broadcastRegistered = true

        val filter = IntentFilter().apply {
            addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            addAction("android.intent.action.SIM_STATE_CHANGED")
        }
        EzXposed.appContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val slot0SubId = subSlotIndices.entries.firstOrNull { (_, slotIndex) ->
                    slotIndex == 0
                }?.key ?: -1
                showNameFlowProxy.syncFromBroadcast(slot0SubId)
                mobileTypeSingleVisibleProxy.syncFromBroadcast(slot0SubId)
                mobileTypeVisibleProxy.syncFromBroadcast(slot0SubId)
                scheduleRefreshBoundViews()
            }
        }, filter)
    }

    @SuppressLint("NewApi")
    private fun setOnDefaultConnectionsListener(mobileUiAdapter: Any) {
        val miuiInt = if (isMoreAndroidVersion(36)) {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiIntsLazy")
                .callMethodOrNull("get")
        } else {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiInt")
        } ?: return

        val defaultConnections = miuiInt.getObjectFieldAs<Any>("connectRepo")
            .getObjectFieldAs<Any>("defaultConnections")

        if (defaultConnectionsCollectorSource !== miuiInt) {
            defaultConnectionsCollectorJob?.cancel()
            runCatching {
                isWifiDefaultConnection = getStateFlowValue(defaultConnections)
                    ?.getObjectField("wifi")
                    ?.getBooleanField("isDefault") == true
            }.onFailure {
                isWifiDefaultConnection = null
            }
            defaultConnectionsCollectorJob = MiuiStub.javaAdapter.alwaysCollectFlow(
                defaultConnections,
                Consumer<Any> { conn ->
                    isWifiDefaultConnection = runCatching {
                        conn.getObjectField("wifi")?.getBooleanField("isDefault")
                    }.getOrNull()
                    scheduleRefreshBoundViews()
                }
            )
            defaultConnectionsCollectorSource = miuiInt
        }
    }

    @SuppressLint("NewApi")
    private fun setOnDataChangedListener(mobileUiAdapter: Any) {
        val miuiInt = if (isMoreAndroidVersion(36)) {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiIntsLazy")
                .callMethodOrNull("get")
        } else {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiInt")
        } ?: return

        val defaultConnections = miuiInt.getObjectFieldAs<Any>("connectRepo")
            .getObjectFieldAs<Any>("defaultConnections")
        val dataConnected = miuiInt.getObjectFieldAs<Any>("dataConnected")

        if (dataChangedCollectorSource !== miuiInt) {
            dataChangedCollectorJob?.cancel()
            dataChangedCollectorJob = MiuiStub.javaAdapter.alwaysCollectFlow(dataConnected, Consumer<BooleanArray> { states ->
                subSlotIndices.forEach { (subId, slotIndex) ->
                    dataConnectedStates[subId] = slotIndex in states.indices && states[slotIndex]
                }
                val isNoDataConnected = when (states.size) {
                    1 -> !states[0]
                    2 -> !states[0] && !states[1]
                    else -> false
                }
                isAnyDataConnected = !isNoDataConnected

                boundViews.values.forEach { viewSet ->
                    val iter = viewSet.iterator()
                    while (iter.hasNext()) {
                        val view = iter.next()
                        if (!view.isAttachedToWindow) {
                            iter.remove()
                            continue
                        }
                        val subId = runCatching { view.getIntField("subId") }.getOrDefault(-1)
                        if (subId == -1) continue
                        runCatching {
                            setSubId(view, subId, isNoDataConnected, defaultConnections, states)
                        }.onFailure { e ->
                            XposedLog.e(TAG, lpparam.packageName, "setOnDataChangedListener error: ${e.message}")
                        }
                    }
                }
                scheduleRefreshBoundViews()
            })
            dataChangedCollectorSource = miuiInt
        }
    }

    private fun setSubId(
        view: View,
        subId: Int,
        isNoDataConnected: Boolean,
        defaultConnections: Any,
        booleans: BooleanArray
    ) {
        // 大 5G 模式已移除 mobile_type ImageView，无需处理
        if (showMobileType) return

        val containerLeft: ViewGroup
        var containerRight: ViewGroup? = null
        if (isMoreOS3) {
            containerLeft = view.findViewByIdName("mobile_signal_container") as ViewGroup
        } else {
            containerLeft = view.findViewByIdName("mobile_container_left") as ViewGroup
            containerRight = view.findViewByIdName("mobile_container_right") as ViewGroup
        }

        val isDefaultDataSim = subId == SubscriptionManager.getDefaultDataSubscriptionId()
        val isSlot0InDouble = isEnableDouble && SubscriptionManager.getSlotIndex(subId) == 0

        if (isDefaultDataSim || isSlot0InDouble) {
            // 上网卡（或双排模式的 slot 0）
            if (mobileNetworkType == 4 && !isEnableDouble) {
                (view.findViewByIdName("mobile_type") as? ImageView)?.isVisible = !isNoDataConnected
            }
            // OS2: 调整左右容器边距，补偿隐藏元素的间距
            // OS3: ConstraintLayout 不需要 padding 补偿
            if (!isMoreOS3 && !isEnableDouble) {
                val slotIndex = SubscriptionManager.getSlotIndex(subId)
                val isSlotDataConnected = slotIndex in booleans.indices && booleans[slotIndex]
                val isWifiDefault = runCatching {
                    getStateFlowValue(defaultConnections)?.getObjectField("wifi")?.getBooleanField("isDefault")
                }.getOrNull() == true
                val needPadding = isNoDataConnected || (isWifiDefault && !isSlotDataConnected)
                val paddingLeft = if (needPadding) 20 else 0
                containerLeft.setPadding(if (isNoDataConnected) paddingLeft else 0, 0, 0, 0)
                containerRight?.setPadding(paddingLeft, 0, 0, 0)
            }
        } else if (!isEnableDouble) {
            // 非上网卡、非双排模式
            if (mobileNetworkType == 4) {
                (view.findViewByIdName("mobile_type") as? ImageView)?.isVisible = false
            }
            if (!isMoreOS3) {
                containerLeft.setPadding(20, 0, 0, 0)
                containerRight?.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun registerMobileStateCollectors(viewModel: Any, interactor: Any?, subId: Int, slotIndex: Int) {
        subSlotIndices[subId] = slotIndex
        inheritStateForReplacementSubId(subId, slotIndex)

        collectRenderState(
            viewModel.getObjectField("showName")
        ) { value ->
            val showName = value as? String ?: ""
            showNameStates[subId] = showName
            if (showName.isNotEmpty()) {
                showNameReadyStates[subId] = true
            }
        }
        collectRenderState(
            viewModel.getObjectField("inOutVisible")
        ) { value ->
            inOutVisibleStates[subId] = value as? Boolean ?: false
        }
        collectRenderState(
            interactor?.getObjectField("isDataConnected")
        ) { value ->
            dataConnectedStates[subId] = value as? Boolean ?: false
        }
        collectRenderState(
            viewModel.getObjectField("wifiAvailable")
        ) { value ->
            wifiAvailableStates[subId] = value as? Boolean ?: false
        }
        collectRenderState(
            viewModel.getObjectField("mobileTypeSingleVisible")
        ) { value ->
            mobileTypeSingleVisibleStates[subId] = value as? Boolean ?: false
        }
        collectRenderState(
            viewModel.getObjectField("mobileTypeVisible")
        ) { value ->
            mobileTypeVisibleStates[subId] = value as? Boolean ?: false
        }
    }

    private fun inheritStateForReplacementSubId(subId: Int, slotIndex: Int) {
        if (slotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) return
        if (showNameReadyStates[subId] == true || hasTrackedState(subId)) return

        val sourceSubId = subSlotIndices.entries.firstOrNull { (oldSubId, oldSlotIndex) ->
            oldSubId != subId && oldSlotIndex == slotIndex && hasTrackedState(oldSubId)
        }?.key ?: return

        showNameStates[sourceSubId]?.takeIf { it.isNotEmpty() }?.let {
            showNameStates[subId] = it
            showNameReadyStates[subId] = true
        }
        inOutVisibleStates[sourceSubId]?.let { inOutVisibleStates[subId] = it }
        dataConnectedStates[sourceSubId]?.let { dataConnectedStates[subId] = it }
        wifiAvailableStates[sourceSubId]?.let { wifiAvailableStates[subId] = it }
        mobileTypeSingleVisibleStates[sourceSubId]?.let { mobileTypeSingleVisibleStates[subId] = it }
        mobileTypeVisibleStates[sourceSubId]?.let { mobileTypeVisibleStates[subId] = it }
    }

    private fun collectRenderState(flow: Any?, update: (Any?) -> Unit) {
        if (flow == null) return
        runCatching { update(getStateFlowValue(flow)) }
        MiuiStub.javaAdapter.alwaysCollectFlow(flow, Consumer<Any?> { value ->
            update(value)
            scheduleRefreshBoundViews()
        })
    }

    private fun cacheBoundView(subId: Int, rootView: ViewGroup) {
        val views = boundViews.computeIfAbsent(subId) { linkedSetOf() }
        val iter = views.iterator()
        while (iter.hasNext()) {
            val view = iter.next()
            if (!view.isAttachedToWindow || view === rootView) {
                iter.remove()
            }
        }
        views.add(rootView)
    }

    private fun scheduleRefreshBoundViews() {
        mainHandler.removeCallbacks(refreshBoundViewsRunnable)
        mainHandler.post(refreshBoundViewsRunnable)
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun refreshBoundViewsNow() {
        boundViews.values.forEach { viewSet ->
            val iter = viewSet.iterator()
            while (iter.hasNext()) {
                val rootView = iter.next()
                if (!rootView.isAttachedToWindow) {
                    iter.remove()
                    continue
                }
                applyBoundViewState(rootView)
            }
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun applyBoundViewState(rootView: ViewGroup) {
        val viewSubId = runCatching { rootView.getIntField("subId") }.getOrDefault(-1)
        if (viewSubId == -1) return

        val targetSubId = resolveRenderSubId(viewSubId)
        val showName = showNameStates[targetSubId] ?: ""
        val inOutVisible = inOutVisibleStates[targetSubId] ?: false
        val mobileTypeSingleVisible = mobileTypeSingleVisibleStates[targetSubId] ?: false
        val mobileTypeVisible = mobileTypeVisibleStates[targetSubId] ?: false
        val resolvedSmallMobileTypeVisible = resolveSmallMobileTypeVisibility(targetSubId, mobileTypeVisible)

        (rootView.findViewByIdName("mobile_type_single") as? TextView)?.let { textView ->
            val previousText = textView.text?.toString().orEmpty()
            val displayName = showName.ifEmpty { previousText }
            if (showName.isNotEmpty() && previousText != showName) {
                textView.text = showName
            }
            if (showMobileType) {
                val shouldShowLargeType =
                    resolveLargeMobileTypeVisibility(mobileTypeSingleVisible) && displayName.isNotEmpty()
                textView.isVisible = shouldShowLargeType
            }
        }

        (rootView.findViewByIdName("mobile_left_mobile_inout") as? ImageView)?.let { inOut ->
            if (!hideIndicator) {
                inOut.isVisible = inOutVisible
            }
        }

        if (!showMobileType && mobileNetworkType == 4) {
            (rootView.findViewByIdName("mobile_type") as? ImageView)?.let { mobileType ->
                mobileType.isVisible = resolvedSmallMobileTypeVisible
                if (!shouldUseDualRowDataSimSync() || shouldRefreshSmallMobileTypeDrawable(resolvedSmallMobileTypeVisible, showName)) {
                    updateMobileTypeDrawable(mobileType, showName)
                }
            }
        } else if (shouldRefreshSmallMobileTypeDrawable(resolvedSmallMobileTypeVisible, showName)) {
            (rootView.findViewByIdName("mobile_type") as? ImageView)?.let { mobileType ->
                updateMobileTypeDrawable(mobileType, showName)
            }
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun resolveRenderSubId(viewSubId: Int): Int {
        if (!isEnableDouble || MobileViewHelper.isSingleSimMode()) {
            return viewSubId
        }

        val slotIndex = SubscriptionManager.getSlotIndex(viewSubId)
        if (slotIndex != 0) {
            return viewSubId
        }

        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (isRenderStateReady(defaultDataSubId)) {
            lastResolvedDataSubId = defaultDataSubId
            return defaultDataSubId
        }

        if (isRenderStateReady(lastResolvedDataSubId)) {
            return lastResolvedDataSubId
        }

        return if (isRenderStateReady(viewSubId)) viewSubId else if (hasTrackedState(defaultDataSubId)) defaultDataSubId else viewSubId
    }

    private fun hasTrackedState(subId: Int): Boolean {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return false
        return showNameStates.containsKey(subId) ||
            inOutVisibleStates.containsKey(subId) ||
            dataConnectedStates.containsKey(subId) ||
            mobileTypeSingleVisibleStates.containsKey(subId) ||
            mobileTypeVisibleStates.containsKey(subId)
    }

    private fun isRenderStateReady(subId: Int?): Boolean {
        if (subId == null || !SubscriptionManager.isValidSubscriptionId(subId)) return false
        if (!hasTrackedState(subId)) return false
        return !showMobileType || showNameReadyStates[subId] == true
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun shouldUseDualRowDataSimSync(): Boolean {
        return isEnableDouble && !MobileViewHelper.isSingleSimMode()
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun resolveLargeMobileTypeVisibility(mobileTypeSingleVisible: Boolean): Boolean {
        if (!showMobileType) return false
        if (!shouldUseDualRowDataSimSync()) {
            return mobileTypeSingleVisible
        }
        return when (mobileNetworkType) {
            0, 2 -> isWifiDefaultConnection?.not() ?: mobileTypeSingleVisible
            else -> mobileTypeSingleVisible
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun resolveSmallMobileTypeVisibility(targetSubId: Int, mobileTypeVisible: Boolean): Boolean {
        if (showMobileType || !shouldUseDualRowDataSimSync()) {
            return mobileTypeVisible
        }
        return when (mobileNetworkType) {
            4 -> {
                val wifiConnected = wifiAvailableStates[targetSubId]
                val dataConnected = isAnyDataConnected
                when {
                    wifiConnected == true -> false
                    dataConnected != null -> dataConnected
                    else -> mobileTypeVisible
                }
            }
            else -> mobileTypeVisible
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun shouldRefreshSmallMobileTypeDrawable(mobileTypeVisible: Boolean, showName: String): Boolean {
        return !showMobileType &&
            shouldUseDualRowDataSimSync() &&
            (mobileNetworkType == 0 || mobileNetworkType == 2 || mobileNetworkType == 4) &&
            (mobileNetworkType == 0 || mobileTypeVisible) &&
            showName.isNotEmpty()
    }

    private fun updateMobileTypeDrawable(imageView: ImageView, showName: String) {
        val drawable = imageView.drawable ?: return
        runCatching {
            val currentName = drawable.getObjectFieldAs<String>("mMobileType")
            if (currentName != showName) {
                drawable.setObjectField("mMobileType", showName)
                drawable.callMethodOrNull("measure")
                miuiMobileIconBinder.callStaticMethod("updateMobileTypeLayoutParams", drawable, showName, imageView)
                drawable.callMethodOrNull("invalidateSelf")
            }
        }.onFailure {
            XposedLog.w(TAG, lpparam.packageName, "updateMobileTypeDrawable failed: ${it.message}")
        }
    }
}
