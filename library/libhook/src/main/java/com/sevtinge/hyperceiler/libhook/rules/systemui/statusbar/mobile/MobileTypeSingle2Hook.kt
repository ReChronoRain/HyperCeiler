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
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.view.isVisible
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.support.MobileTypeRenderStateStore
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.support.MobileTypeViewRenderer
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.support.MobileTypeVisibilityResolver
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion
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
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

object MobileTypeSingle2Hook : BaseHook() {
    private const val LAST_BOUND_VIEW_MODEL_KEY = "mobile_type_single2_last_bound_vm"

    private val showNameFlowProxy = DataSimFlowProxy("")
    private val inOutVisibleProxy = DataSimFlowProxy(false)
    private val inOutResIdProxy = DataSimFlowProxy(0)
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

    private val boundViews = ConcurrentHashMap<Int, MutableSet<ViewGroup>>()
    private val renderStateStore = MobileTypeRenderStateStore()
    private val visibilityResolver = MobileTypeVisibilityResolver(
        showMobileType = showMobileType,
        mobileNetworkType = mobileNetworkType,
        isEnableDouble = isEnableDouble,
        isSingleSimMode = MobileViewHelper::isSingleSimMode
    )
    private val viewRenderer = MobileTypeViewRenderer(
        showMobileType = showMobileType,
        hideIndicator = hideIndicator,
        mobileNetworkType = mobileNetworkType,
        visibilityResolver = visibilityResolver,
        updateMobileTypeDrawable = ::updateMobileTypeDrawable
    )

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    @SuppressLint("MissingPermission")
    private val refreshBoundViewsRunnable = Runnable { refreshBoundViewsNow() }

    override fun init() {
        hookMobileViewAndVM()
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

            val subId = runCatching {
                miuiInteractor?.getObjectFieldAs<Int>("subId")
            }.getOrNull() ?: runCatching {
                interactor?.getObjectFieldAs<Int>("subId")
            }.getOrNull() ?: runCatching {
                viewModel.getObjectFieldAs<Int>("subId")
            }.getOrNull() ?: return@createAfterHook

            val slotIndex = SubscriptionManager.getSlotIndex(subId)
            if (isEnableDouble) {
                viewModel.getObjectField("showName")?.let { originalFlow ->
                    showNameFlowProxy.setupForSlot(slotIndex, subId, originalFlow, MobileViewHelper::isSingleSimMode)
                    if (slotIndex == 0) {
                        viewModel.setObjectField("showName", showNameFlowProxy.proxy!!)
                    }
                }
                if (!hideIndicator) {
                    viewModel.getObjectField("inOutVisible")?.let { originalFlow ->
                        inOutVisibleProxy.setupForSlot(slotIndex, subId, originalFlow, MobileViewHelper::isSingleSimMode)
                        if (slotIndex == 0) {
                            viewModel.setObjectField("inOutVisible", inOutVisibleProxy.proxy!!)
                        }
                    }
                    viewModel.getObjectField("inOutResId")?.let { originalFlow ->
                        inOutResIdProxy.setupForSlot(slotIndex, subId, originalFlow, MobileViewHelper::isSingleSimMode)
                        if (slotIndex == 0) {
                            viewModel.setObjectField("inOutResId", inOutResIdProxy.proxy!!)
                        }
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
            }

            registerMobileStateCollectors(
                viewModel,
                interactor,
                subId,
                slotIndex
            )

            if (isEnableDouble) {
                syncDataSimProxiesNow()
                registerDataSimBroadcast()
            }
            scheduleRefreshBoundViews()
        }

        modernStatusBarMobileView.methodFinder()
            .filterByName("constructAndBind")
            .toList()
            .forEach { method ->
                method.hook {
                    val result = proceed()
                    val rootView = result as? ViewGroup ?: return@hook result
                    val viewModel = resolveConstructAndBindViewModel(args) ?: return@hook result
                    bindConstructedMobileViewIfNeeded(rootView, viewModel)
                    result
                }
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

    private fun resolveConstructAndBindViewModel(args: List<Any?>): Any? {
        args.asReversed().forEach { arg ->
            val candidate = unwrapCellProviderViewModel(arg) ?: return@forEach
            val simpleName = candidate.javaClass.simpleName
            if (simpleName.contains("MobileIconVM", ignoreCase = true) ||
                simpleName.contains("CellularIcon", ignoreCase = true)
            ) {
                return candidate
            }
            val hasShowName = runCatching { candidate.getObjectField("showName") != null }.getOrDefault(false)
            val hasLargeVisible = runCatching { candidate.getObjectField("mobileTypeSingleVisible") != null }.getOrDefault(false)
            val hasSmallVisible = runCatching { candidate.getObjectField("mobileTypeVisible") != null }.getOrDefault(false)
            if (hasShowName && (hasLargeVisible || hasSmallVisible)) {
                return candidate
            }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun safeIsWifiConnected(): Boolean? {
        return runCatching { MobileViewHelper.isWifiConnected() }.getOrNull()
    }

    private fun normalizeWifiDefaultConnection(rawIsWifiDefault: Boolean?, wifiConnectedNow: Boolean?): Boolean? {
        return when {
            rawIsWifiDefault == true && wifiConnectedNow == false -> false
            rawIsWifiDefault != null -> rawIsWifiDefault
            else -> wifiConnectedNow
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun bindConstructedMobileViewIfNeeded(rootView: ViewGroup, viewModel: Any) {
        val lastBoundViewModel = runCatching {
            rootView.getAdditionalInstanceFieldAs<Any>(LAST_BOUND_VIEW_MODEL_KEY)
        }.getOrNull()
        if (lastBoundViewModel === viewModel) return
        rootView.setAdditionalInstanceField(LAST_BOUND_VIEW_MODEL_KEY, viewModel)
        bindConstructedMobileView(rootView, viewModel)
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun bindConstructedMobileView(rootView: ViewGroup, viewModel: Any) {
        val interactor = viewModel.getAdditionalInstanceFieldAs<Any>("interactor")
        val subId = rootView.getIntField("subId")
        val slotIndex = SubscriptionManager.getSlotIndex(subId)
        if (slotIndex == -1) return
        cacheBoundView(subId, rootView)

        val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout
        val containerLeft = mobileGroup.findViewByIdName("mobile_signal_container") as ViewGroup

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
            if (isEnableDouble && slotIndex != 0) {
                return
            }
            if ((mobileNetworkType == 0 || mobileNetworkType == 2) && isEnableDouble) {
                syncWifiDefaultConnectionSnapshot(interactor)
            }

            when (mobileNetworkType) {
                0, 2 -> {
                    viewModel.setObjectField("mobileTypeSingleVisible", newReadonlyStateFlow(false))

                    val defaultConnections = runCatching {
                        interactor?.getObjectFieldAs<Any>("connectRepo")
                            ?.getObjectFieldAs<Any>("defaultConnections")
                    }.getOrNull()

                    if (defaultConnections != null) {
                        bindMobileTypeSingleVisibilityWithDefaultConnections(
                            viewModel = viewModel,
                            defaultConnections = defaultConnections,
                            subId = subId
                        )
                    } else {
                        bindMobileTypeSingleVisibilityWithWifiFlow(
                            viewModel = viewModel,
                            wifiFlow = viewModel.getObjectFieldAs("wifiAvailable"),
                            subId = subId
                        )
                    }
                }
                1 -> viewModel.setObjectField("mobileTypeSingleVisible", newReadonlyStateFlow(true))
                3 -> viewModel.setObjectField("mobileTypeSingleVisible", newReadonlyStateFlow(false))
                else -> Unit
            }
            applyBoundViewState(rootView)
            return
        }

        // ===== 小 5G 可见性 =====
        // 双排的 slot 1+ 整个视图已被 MobilePublicHookV 隐藏
        if (isEnableDouble && slotIndex != 0) {
            return
        }

        when (mobileNetworkType) {
            2 -> {
                val wifiFlow = viewModel.getObjectFieldAs<Any>("wifiAvailable")
                val initWifiOn = runCatching { getStateFlowValue(wifiFlow) as Boolean }
                    .getOrElse { safeIsWifiConnected() ?: false }
                val flow = newReadonlyStateFlow(!initWifiOn)
                viewModel.setObjectField("mobileTypeVisible", flow)
                MiuiStub.javaAdapter.alwaysCollectFlow(
                    wifiFlow,
                    Consumer<Boolean> { wifiOn -> setStateFlowValue(flow, !wifiOn) }
                )
            }
            1 -> viewModel.setObjectField("mobileTypeVisible", newReadonlyStateFlow(true))
            3 -> viewModel.setObjectField("mobileTypeVisible", newReadonlyStateFlow(false))
            4 -> {
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
            else -> Unit
        }

        applyBoundViewState(rootView)
    }

    private fun bindMobileTypeSingleVisibilityWithWifiFlow(viewModel: Any, wifiFlow: Any, subId: Int) {
        val visibleFlow = viewModel.getObjectField("mobileTypeSingleVisible")
        val wifiFromFlow = runCatching { getStateFlowValue(wifiFlow) as Boolean }.getOrNull()
        val wifiConnectedNow = safeIsWifiConnected() ?: wifiFromFlow ?: false
        val initialVisible = !wifiConnectedNow
        setStateFlowValue(visibleFlow, initialVisible)
        renderStateStore.updateMobileTypeSingleVisible(subId, initialVisible)
        MiuiStub.javaAdapter.alwaysCollectFlow(
            wifiFlow,
            Consumer<Boolean> { wifiOn ->
                val visible = !wifiOn
                setStateFlowValue(visibleFlow, visible)
                renderStateStore.updateMobileTypeSingleVisible(subId, visible)
            }
        )
    }

    private fun bindMobileTypeSingleVisibilityWithDefaultConnections(viewModel: Any, defaultConnections: Any, subId: Int) {
        val visibleFlow = viewModel.getObjectField("mobileTypeSingleVisible")
        val initialIsWifiDefault = runCatching {
            getStateFlowValue(defaultConnections)
                ?.getObjectField("wifi")
                ?.getBooleanField("isDefault")
        }.getOrNull() ?: (safeIsWifiConnected() ?: false)
        val initialVisible = !initialIsWifiDefault
        setStateFlowValue(visibleFlow, initialVisible)
        renderStateStore.updateMobileTypeSingleVisible(subId, initialVisible)
        MiuiStub.javaAdapter.alwaysCollectFlow(
            defaultConnections,
            Consumer<Any> { conn ->
                val isWifiDefault = runCatching {
                    conn.getObjectField("wifi")?.getBooleanField("isDefault")
                }.getOrNull() ?: (safeIsWifiConnected() ?: false)
                val visible = !isWifiDefault
                setStateFlowValue(visibleFlow, visible)
                renderStateStore.updateMobileTypeSingleVisible(subId, visible)
            }
        )
    }

    private fun syncWifiDefaultConnectionSnapshot(interactor: Any?) {
        val wifiConnectedNow = safeIsWifiConnected()
        val currentIsWifiDefault = runCatching {
            interactor?.getObjectFieldAs<Any>("connectRepo")
                ?.getObjectFieldAs<Any>("defaultConnections")
                ?.let { defaultConnections ->
                    getStateFlowValue(defaultConnections)
                        ?.getObjectField("wifi")
                        ?.getBooleanField("isDefault")
                }
        }.getOrNull()
        isWifiDefaultConnection = normalizeWifiDefaultConnection(currentIsWifiDefault, wifiConnectedNow)
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
                syncDataSimProxiesNow()
                scheduleRefreshBoundViews()
            }
        }, filter, Context.RECEIVER_EXPORTED)
    }

    private fun syncDataSimProxiesNow() {
        val slot0SubId = renderStateStore.findSlot0SubId()
        showNameFlowProxy.syncFromBroadcast(slot0SubId)
        inOutVisibleProxy.syncFromBroadcast(slot0SubId)
        inOutResIdProxy.syncFromBroadcast(slot0SubId)
        mobileTypeSingleVisibleProxy.syncFromBroadcast(slot0SubId)
        mobileTypeVisibleProxy.syncFromBroadcast(slot0SubId)
    }

    @SuppressLint("NewApi")
    private fun setOnDefaultConnectionsListener(mobileUiAdapter: Any) {
        val miuiInt = runCatching {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiIntsLazy")
                .callMethodOrNull("get")
        }.recoverCatching {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiInt")
        }.getOrNull() ?: return

        val defaultConnections = miuiInt.getObjectFieldAs<Any>("connectRepo")
            .getObjectFieldAs<Any>("defaultConnections")

        if (defaultConnectionsCollectorSource !== miuiInt) {
            defaultConnectionsCollectorJob?.cancel()
            val initialRawIsWifiDefault = runCatching {
                getStateFlowValue(defaultConnections)
                    ?.getObjectField("wifi")
                    ?.getBooleanField("isDefault")
            }.getOrNull()
            isWifiDefaultConnection = normalizeWifiDefaultConnection(initialRawIsWifiDefault, safeIsWifiConnected())
            scheduleRefreshBoundViews()
            defaultConnectionsCollectorJob = MiuiStub.javaAdapter.alwaysCollectFlow(
                defaultConnections,
                Consumer<Any> { conn ->
                    val rawIsWifiDefault = runCatching {
                        conn.getObjectField("wifi")?.getBooleanField("isDefault")
                    }.getOrNull()
                    isWifiDefaultConnection = normalizeWifiDefaultConnection(rawIsWifiDefault, safeIsWifiConnected())
                    scheduleRefreshBoundViews()
                }
            )
            defaultConnectionsCollectorSource = miuiInt
        }
    }

    @SuppressLint("NewApi")
    private fun setOnDataChangedListener(mobileUiAdapter: Any) {
        val miuiInt = runCatching {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiIntsLazy")
                .callMethodOrNull("get")
        }.recoverCatching {
            mobileUiAdapter.getObjectFieldAs<Any>("mobileIconsViewModel")
                .getObjectFieldAs<Any>("miuiInt")
        }.getOrNull() ?: return

        val dataConnected = miuiInt.getObjectFieldAs<Any>("dataConnected")

        if (dataChangedCollectorSource !== miuiInt) {
            dataChangedCollectorJob?.cancel()
            dataChangedCollectorJob = MiuiStub.javaAdapter.alwaysCollectFlow(dataConnected, Consumer<BooleanArray> { states ->
                renderStateStore.updateDataConnectedBySlots(states)
                scheduleRefreshBoundViews()
            })
            dataChangedCollectorSource = miuiInt
        }
    }

    private fun registerMobileStateCollectors(viewModel: Any, interactor: Any?, subId: Int, slotIndex: Int) {
        renderStateStore.onCollectorAttached(subId, slotIndex)

        collectRenderState(
            viewModel.getObjectField("showName")
        ) { value ->
            renderStateStore.updateShowName(subId, value as? String ?: "")
        }
        collectRenderState(
            viewModel.getObjectField("inOutVisible")
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateInOutVisible(subId, it) }
        }
        collectRenderState(
            interactor?.getObjectField("isDataConnected")
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateDataConnected(subId, it) }
        }
        collectRenderState(
            viewModel.getObjectField("wifiAvailable")
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateWifiAvailable(subId, it) }
        }
        collectRenderState(
            viewModel.getObjectField("mobileTypeSingleVisible")
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateMobileTypeSingleVisible(subId, it) }
        }
        collectRenderState(
            viewModel.getObjectField("mobileTypeVisible")
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateMobileTypeVisible(subId, it) }
        }
    }

    private fun coerceBooleanState(value: Any?): Boolean? {
        return when (value) {
            null -> null
            is Boolean -> value
            else -> {
                runCatching { value.getObjectFieldAs<Boolean>("first") }.getOrNull()
                    ?: runCatching { value.callMethodAs<Boolean>("component1") }.getOrNull()
                    ?: runCatching { value.getBooleanField("value") }.getOrNull()
            }
        }
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

        val targetSubId = renderStateStore.resolveRenderSubId(
            viewSubId = viewSubId,
            isEnableDouble = isEnableDouble,
            isSingleSimMode = MobileViewHelper.isSingleSimMode()
        )
        val renderState = renderStateStore.snapshot(targetSubId)

        val mobileTypeSingleView = rootView.findViewByIdName("mobile_type_single") as? TextView
        val mobileTypeSmallView = rootView.findViewByIdName("mobile_type") as? ImageView
        val inOutView = rootView.findViewByIdName("mobile_left_mobile_inout") as? ImageView

        val wifiConnectedNow = safeIsWifiConnected()
        val normalizedWifiDefaultConnection = normalizeWifiDefaultConnection(isWifiDefaultConnection, wifiConnectedNow)
        val effectiveWifiDefaultConnection = normalizedWifiDefaultConnection ?: wifiConnectedNow
        val resolvedLargeVisible = visibilityResolver.resolveLargeMobileTypeVisibility(
            mobileTypeSingleVisible = renderState.mobileTypeSingleVisible,
            isWifiDefaultConnection = effectiveWifiDefaultConnection,
            fallbackVisible = mobileTypeSingleView?.isVisible ?: false,
            forceDualMode = isEnableDouble && (mobileNetworkType == 0 || mobileNetworkType == 2)
        )
        val effectiveWifiConnected = if (!showMobileType && mobileNetworkType == 2) {
            safeIsWifiConnected() ?: renderState.wifiConnected
        } else {
            renderState.wifiConnected ?: safeIsWifiConnected()
        }
        val resolvedSmallVisible = visibilityResolver.resolveSmallMobileTypeVisibility(
            mobileTypeVisible = renderState.mobileTypeVisible,
            wifiConnected = effectiveWifiConnected,
            dataConnected = renderState.dataConnected,
            fallbackVisible = mobileTypeSmallView?.isVisible ?: false
        )

        viewRenderer.applyLargeMobileType(
            textView = mobileTypeSingleView,
            showName = renderState.showName,
            largeTypeVisible = resolvedLargeVisible
        )
        if (!visibilityResolver.shouldUseDualRowDataSimSync()) {
            viewRenderer.applyInOut(
                indicatorView = inOutView,
                inOutVisible = renderState.inOutVisible
            )
        }
        viewRenderer.applySmallMobileType(
            imageView = mobileTypeSmallView,
            smallVisible = resolvedSmallVisible,
            showName = renderState.showName
        )
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
