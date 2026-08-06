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
import io.github.lingqiqi5211.ezhooktool.core.callMethodAs
import io.github.lingqiqi5211.ezhooktool.core.callMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findViewByIdName
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getAdditionalInstanceFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getBooleanField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getIntField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setAdditionalInstanceField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createInterceptHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

object MobileTypeSingle2Hook : BaseHook() {
    private const val LAST_BOUND_VIEW_MODEL_KEY = "mobile_type_single2_last_bound_vm"
    private const val DATA_SIM_CONTEXT_KEY = "MobileTypeSingle2Hook.dataSimContext"

    private const val FIELD_SHOW_NAME = "showName"
    private const val FIELD_IN_OUT_VISIBLE = "inOutVisible"
    private const val FIELD_IN_OUT_RES_ID = "inOutResId"
    private const val FIELD_MOBILE_TYPE_SINGLE_VISIBLE = "mobileTypeSingleVisible"
    private const val FIELD_MOBILE_TYPE_VISIBLE = "mobileTypeVisible"
    private const val FIELD_WIFI_AVAILABLE = "wifiAvailable"
    private const val FIELD_SUB_ID = "subId"
    private const val FIELD_IS_DATA_CONNECTED = "isDataConnected"
    private const val FIELD_CONNECT_REPO = "connectRepo"
    private const val FIELD_DEFAULT_CONNECTIONS = "defaultConnections"
    private const val FIELD_MOBILE_ICONS_VIEW_MODEL = "mobileIconsViewModel"
    private const val FIELD_WIFI = "wifi"
    private const val FIELD_IS_DEFAULT = "isDefault"
    private const val VIEW_MOBILE_TYPE_SINGLE = "mobile_type_single"
    private const val VIEW_MOBILE_TYPE = "mobile_type"

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

    // VMs (by identityHashCode) already handled on the bind path, so the flows are not
    // replaced again for every location.
    private val appliedViewModels = ConcurrentHashMap.newKeySet<Int>()

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
        BaseHook.registerHandlerHotReloadCleanup(mainHandler)
        if (isEnableDouble) {
            getHotReloadRuntimeState(DATA_SIM_CONTEXT_KEY, Context::class.java)
                ?.let(::registerDataSimBroadcast)
        }
        hookMobileViewAndVM()
    }

    @SuppressLint("MissingPermission")
    private fun hookMobileViewAndVM() {
        if (showMobileType || mobileNetworkType == 3) {
            if (isMoreAndroidVersion(36)) {
                loadClass("com.miui.systemui.statusbar.views.MobileTypeDrawable")
            } else {
                loadClass("com.android.systemui.statusbar.views.MobileTypeDrawable")
            }.findMethod { name("measure") }.createHook {
                returnConstant(null)
            }
        }

        // OS 2.x / OS 3.0: MiuiCellularIconVM still has an <init>, so keep the original
        // constructor hook.
        if (miuiCellularIconVM.declaredConstructors.isNotEmpty()) {
            Constructors.find(miuiCellularIconVM).first().createAfterHook { param ->
                applyToViewModel(param.thisObject, param.args[1], param.args[2])
            }
        } else {
            // HyperOS 3.3 (Android 17): R8 inlined MiuiCellularIconVM's constructor away,
            // so Constructors.find(...).first() throws MemberNotFoundException and the
            // whole hook is lost. Handle it before MiuiMobileIconBinder#bind instead,
            // where args[2] is the VM. Both interactors that used to be constructor
            // arguments are reachable from the VM itself on the new version:
            //   args[1] interactor     -> originIconInteractor
            //   args[2] miuiInteractor -> only used to read wifiAvailable, and the new VM
            //                             already has that field, so null is passed here
            //                             and the copy is simply skipped.
            miuiMobileIconBinder.findMethod { name("bind") }
                .createBeforeHook { param -> applyOnBind(param.args[2]) }
        }

        hookMobileViewExtras()
    }

    // bind receives a MiuiMobileIconVMImpl wrapper whose inOutVisible / mobileTypeVisible
    // are ChannelFlowTransformLatest (cold flows, no getValue) and which has no
    // wifiAvailable field. The original constructor hook saw the inner MiuiCellularIconVM,
    // where all of these are ReadonlyStateFlow, so unwrap first.
    //
    // bind is called once per location for the same VM; the VM is registered only after
    // success so a single failure does not skip it forever.
    @SuppressLint("MissingPermission")
    private fun applyOnBind(boundViewModel: Any?) {
        val viewModel = unwrapCellProviderViewModel(boundViewModel) ?: return
        val viewModelKey = System.identityHashCode(viewModel)
        if (viewModelKey in appliedViewModels) return
        val interactor = runCatching {
            viewModel.callMethodAs<Any>("getOriginIconInteractor")
        }.getOrNull() ?: return
        applyToViewModel(viewModel, interactor, null)
        appliedViewModels.add(viewModelKey)
    }

    // Resolves the subId.
    //
    // Older versions read subId straight off the interactor passed as a constructor
    // argument. On HyperOS 3.3 that field was renamed to subscriptionId on
    // MobileIconInteractorImpl (with a matching getSubscriptionId()). The old name is
    // tried first, so older versions keep hitting the same branch as before.
    private fun resolveSubId(viewModel: Any, interactor: Any?, miuiInteractor: Any?): Int? {
        val candidates = listOfNotNull(miuiInteractor, interactor, viewModel)
        for (target in candidates) {
            runCatching { target.getObjectFieldAs<Int>(FIELD_SUB_ID) }.getOrNull()?.let { return it }
        }
        for (target in candidates) {
            runCatching { target.getObjectFieldAs<Int>("subscriptionId") }.getOrNull()?.let { return it }
            runCatching { target.callMethodAs<Int>("getSubscriptionId") }.getOrNull()?.let { return it }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun applyToViewModel(viewModel: Any, interactor: Any?, miuiInteractor: Any?) {
        viewModel.setAdditionalInstanceField("interactor", interactor)
        // The new VM already owns a wifiAvailable field, so there is nothing to copy when
        // no miuiInteractor was supplied
        if (miuiInteractor != null) {
            viewModel.setObjectField(FIELD_WIFI_AVAILABLE, miuiInteractor.getObjectField(FIELD_WIFI_AVAILABLE))
        }

        val subId = resolveSubId(viewModel, interactor, miuiInteractor) ?: return

        val slotIndex = SubscriptionManager.getSlotIndex(subId)
        if (isEnableDouble) {
            installDataSimProxy(viewModel, FIELD_SHOW_NAME, showNameFlowProxy, subId, slotIndex)
            if (!hideIndicator) {
                installDataSimProxy(viewModel, FIELD_IN_OUT_VISIBLE, inOutVisibleProxy, subId, slotIndex)
                installDataSimProxy(viewModel, FIELD_IN_OUT_RES_ID, inOutResIdProxy, subId, slotIndex)
            }
            installDataSimProxy(
                viewModel, FIELD_MOBILE_TYPE_SINGLE_VISIBLE, mobileTypeSingleVisibleProxy, subId, slotIndex
            )
            installDataSimProxy(viewModel, FIELD_MOBILE_TYPE_VISIBLE, mobileTypeVisibleProxy, subId, slotIndex)
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

    // Only slot 0 owns the shared data-SIM proxy; every other slot just registers its
    // original flow with the proxy so the broadcast can switch over to it later.
    private fun installDataSimProxy(
        viewModel: Any,
        fieldName: String,
        proxy: DataSimFlowProxy,
        subId: Int,
        slotIndex: Int
    ) {
        val originalFlow = viewModel.getObjectField(fieldName) ?: return
        proxy.setupForSlot(slotIndex, subId, originalFlow, MobileViewHelper::isSingleSimMode)
        if (slotIndex == 0) {
            viewModel.setObjectField(fieldName, proxy.proxy!!)
        }
    }

    private fun hookMobileViewExtras() {
        modernStatusBarMobileView.findAllMethods { name("constructAndBind") }
            .forEach { method ->
                method.createInterceptHook { chain ->
                    val result = chain.proceed()
                    bindConstructedResult(result, chain.args.toList())
                    result
                }
            }

        if (!showMobileType && mobileNetworkType == 4) {
                Constructors.find(mobileUiAdapter).first().createAfterHook {
                setOnDataChangedListener(it.thisObject)
            }
        }

        if (showMobileType && isEnableDouble && (mobileNetworkType == 0 || mobileNetworkType == 2)) {
                Constructors.find(mobileUiAdapter).first().createAfterHook {
                setOnDefaultConnectionsListener(it.thisObject)
            }
        }

        if (showMobileType && mobileNetworkType == 4) {
            showMobileTypeSingle()
        }
    }

    private fun showMobileTypeSingle() {
        // OS 2.x / OS 3.0: OperatorConfig still has an <init>.
        val constructors = mOperatorConfig.constructors
        if (constructors.isNotEmpty()) {
            constructors[0].createAfterHook {
                it.thisObject.setObjectField("showMobileDataTypeSingle", true)
            }
            return
        }

        // HyperOS 3.3 (Android 17): R8 inlined OperatorConfig's constructor into
        // MiuiOperatorCustomizedPolicy#getMiuiOperatorConfig (see HideVoWiFiIcon), so
        // constructors[0] throws ArrayIndexOutOfBoundsException. Hook that method's
        // return value instead.
        loadClass("com.android.systemui.MiuiOperatorCustomizedPolicy")
            .findMethod { name("getMiuiOperatorConfig") }
            .createAfterHook { param ->
                param.result?.setObjectField("showMobileDataTypeSingle", true)
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
    private fun bindConstructedResult(result: Any?, args: List<Any?>) {
        val rootView = result as? ViewGroup ?: return
        val viewModel = resolveConstructAndBindViewModel(args) ?: return
        bindConstructedMobileViewIfNeeded(rootView, viewModel)
    }

    private fun resolveConstructAndBindViewModel(args: List<Any?>): Any? {
        for (arg in args.asReversed()) {
            val candidate = unwrapCellProviderViewModel(arg) ?: continue
            val simpleName = candidate.javaClass.simpleName
            if (simpleName.contains("MobileIconVM", ignoreCase = true) ||
                simpleName.contains("CellularIcon", ignoreCase = true)
            ) {
                return candidate
            }
            val hasShowName = runCatching { candidate.getObjectField(FIELD_SHOW_NAME) != null }.getOrDefault(false)
            val hasLargeVisible =
                runCatching { candidate.getObjectField(FIELD_MOBILE_TYPE_SINGLE_VISIBLE) != null }.getOrDefault(false)
            val hasSmallVisible =
                runCatching { candidate.getObjectField(FIELD_MOBILE_TYPE_VISIBLE) != null }.getOrDefault(false)
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
        val subId = rootView.getIntField(FIELD_SUB_ID)
        val slotIndex = SubscriptionManager.getSlotIndex(subId)
        if (slotIndex == -1) return
        cacheBoundView(subId, rootView)

        val mobileGroup = rootView.findViewByIdName("mobile_group") as LinearLayout
        val containerLeft = mobileGroup.findViewByIdName("mobile_signal_container") as ViewGroup

        // 大 5G 样式：移除小 5G ImageView，配置大 5G TextView
        if (showMobileType) {
            containerLeft.findViewByIdName(VIEW_MOBILE_TYPE)?.let { containerLeft.removeView(it) }
            val textView = mobileGroup.findViewByIdName(VIEW_MOBILE_TYPE_SINGLE) as TextView
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
                    viewModel.setObjectField(FIELD_MOBILE_TYPE_SINGLE_VISIBLE, newReadonlyStateFlow(false))

                    val defaultConnections = runCatching {
                        interactor?.getObjectFieldAs<Any>(FIELD_CONNECT_REPO)
                            ?.getObjectFieldAs<Any>(FIELD_DEFAULT_CONNECTIONS)
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
                            wifiFlow = viewModel.getObjectFieldAs(FIELD_WIFI_AVAILABLE),
                            subId = subId
                        )
                    }
                }
                1 -> viewModel.setObjectField(FIELD_MOBILE_TYPE_SINGLE_VISIBLE, newReadonlyStateFlow(true))
                3 -> viewModel.setObjectField(FIELD_MOBILE_TYPE_SINGLE_VISIBLE, newReadonlyStateFlow(false))
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
                val wifiFlow = viewModel.getObjectFieldAs<Any>(FIELD_WIFI_AVAILABLE)
                val initWifiOn = runCatching { getStateFlowValue(wifiFlow) as Boolean }
                    .getOrElse { safeIsWifiConnected() ?: false }
                val flow = newReadonlyStateFlow(!initWifiOn)
                viewModel.setObjectField(FIELD_MOBILE_TYPE_VISIBLE, flow)
                MiuiStub.javaAdapter.alwaysCollectFlow(
                    wifiFlow,
                    Consumer<Boolean> { wifiOn -> setStateFlowValue(flow, !wifiOn) }
                )
            }
            1 -> viewModel.setObjectField(FIELD_MOBILE_TYPE_VISIBLE, newReadonlyStateFlow(true))
            3 -> viewModel.setObjectField(FIELD_MOBILE_TYPE_VISIBLE, newReadonlyStateFlow(false))
            4 -> {
                val wifiFlow = viewModel.getObjectFieldAs<Any>(FIELD_WIFI_AVAILABLE)
                val dataConnectedFlow = interactor?.getObjectFieldAs<Any>(FIELD_IS_DATA_CONNECTED)

                // 先读当前值
                val initWifiOn = runCatching { getStateFlowValue(wifiFlow) as Boolean }.getOrDefault(false)
                val initDataConnected = runCatching { getStateFlowValue(dataConnectedFlow) as Boolean }.getOrDefault(false)

                val flow = newReadonlyStateFlow(!initWifiOn && initDataConnected)
                viewModel.setObjectField(FIELD_MOBILE_TYPE_VISIBLE, flow)

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
        val visibleFlow = viewModel.getObjectField(FIELD_MOBILE_TYPE_SINGLE_VISIBLE)
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
        val visibleFlow = viewModel.getObjectField(FIELD_MOBILE_TYPE_SINGLE_VISIBLE)
        val initialIsWifiDefault = runCatching {
            getStateFlowValue(defaultConnections)
                ?.getObjectField(FIELD_WIFI)
                ?.getBooleanField(FIELD_IS_DEFAULT)
        }.getOrNull() ?: (safeIsWifiConnected() ?: false)
        val initialVisible = !initialIsWifiDefault
        setStateFlowValue(visibleFlow, initialVisible)
        renderStateStore.updateMobileTypeSingleVisible(subId, initialVisible)
        MiuiStub.javaAdapter.alwaysCollectFlow(
            defaultConnections,
            Consumer<Any> { conn ->
                val isWifiDefault = runCatching {
                    conn.getObjectField(FIELD_WIFI)?.getBooleanField(FIELD_IS_DEFAULT)
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
            interactor?.getObjectFieldAs<Any>(FIELD_CONNECT_REPO)
                ?.getObjectFieldAs<Any>(FIELD_DEFAULT_CONNECTIONS)
                ?.let { defaultConnections ->
                    getStateFlowValue(defaultConnections)
                        ?.getObjectField(FIELD_WIFI)
                        ?.getBooleanField(FIELD_IS_DEFAULT)
                }
        }.getOrNull()
        isWifiDefaultConnection = normalizeWifiDefaultConnection(currentIsWifiDefault, wifiConnectedNow)
    }

    /** 监听上网卡切换 + SIM 变化，刷新已绑定的官方 mobile 布局 */
    @Synchronized
    private fun registerDataSimBroadcast(context: Context = EzXposed.appContext) {
        if (broadcastRegistered) return

        val filter = IntentFilter().apply {
            addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            addAction("android.intent.action.SIM_STATE_CHANGED")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                syncDataSimProxiesNow()
                scheduleRefreshBoundViews()
            }
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        broadcastRegistered = true
        BaseHook.registerReceiverHotReloadCleanup(context, receiver)
        BaseHook.putHotReloadRuntimeState(DATA_SIM_CONTEXT_KEY, context)
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
            mobileUiAdapter.getObjectFieldAs<Any>(FIELD_MOBILE_ICONS_VIEW_MODEL)
                .getObjectFieldAs<Any>("miuiIntsLazy")
                .callMethodOrNull("get")
        }.recoverCatching {
            mobileUiAdapter.getObjectFieldAs<Any>(FIELD_MOBILE_ICONS_VIEW_MODEL)
                .getObjectFieldAs<Any>("miuiInt")
        }.getOrNull() ?: return

        val defaultConnections = miuiInt.getObjectFieldAs<Any>(FIELD_CONNECT_REPO)
            .getObjectFieldAs<Any>(FIELD_DEFAULT_CONNECTIONS)

        if (defaultConnectionsCollectorSource !== miuiInt) {
            defaultConnectionsCollectorJob?.cancel()
            val initialRawIsWifiDefault = runCatching {
                getStateFlowValue(defaultConnections)
                    ?.getObjectField(FIELD_WIFI)
                    ?.getBooleanField(FIELD_IS_DEFAULT)
            }.getOrNull()
            isWifiDefaultConnection = normalizeWifiDefaultConnection(initialRawIsWifiDefault, safeIsWifiConnected())
            scheduleRefreshBoundViews()
            defaultConnectionsCollectorJob = MiuiStub.javaAdapter.alwaysCollectFlow(
                defaultConnections,
                Consumer<Any> { conn ->
                    val rawIsWifiDefault = runCatching {
                        conn.getObjectField(FIELD_WIFI)?.getBooleanField(FIELD_IS_DEFAULT)
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
            mobileUiAdapter.getObjectFieldAs<Any>(FIELD_MOBILE_ICONS_VIEW_MODEL)
                .getObjectFieldAs<Any>("miuiIntsLazy")
                .callMethodOrNull("get")
        }.recoverCatching {
            mobileUiAdapter.getObjectFieldAs<Any>(FIELD_MOBILE_ICONS_VIEW_MODEL)
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
            viewModel.getObjectField(FIELD_SHOW_NAME)
        ) { value ->
            renderStateStore.updateShowName(subId, value as? String ?: "")
        }
        collectRenderState(
            viewModel.getObjectField(FIELD_IN_OUT_VISIBLE)
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateInOutVisible(subId, it) }
        }
        collectRenderState(
            interactor?.getObjectField(FIELD_IS_DATA_CONNECTED)
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateDataConnected(subId, it) }
        }
        collectRenderState(
            viewModel.getObjectField(FIELD_WIFI_AVAILABLE)
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateWifiAvailable(subId, it) }
        }
        collectRenderState(
            viewModel.getObjectField(FIELD_MOBILE_TYPE_SINGLE_VISIBLE)
        ) { value ->
            coerceBooleanState(value)?.let { renderStateStore.updateMobileTypeSingleVisible(subId, it) }
        }
        collectRenderState(
            viewModel.getObjectField(FIELD_MOBILE_TYPE_VISIBLE)
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
        val viewSubId = runCatching { rootView.getIntField(FIELD_SUB_ID) }.getOrDefault(-1)
        if (viewSubId == -1) return

        val targetSubId = renderStateStore.resolveRenderSubId(
            viewSubId = viewSubId,
            isEnableDouble = isEnableDouble,
            isSingleSimMode = MobileViewHelper.isSingleSimMode()
        )
        val renderState = renderStateStore.snapshot(targetSubId)

        val mobileTypeSingleView = rootView.findViewByIdName(VIEW_MOBILE_TYPE_SINGLE) as? TextView
        val mobileTypeSmallView = rootView.findViewByIdName(VIEW_MOBILE_TYPE) as? ImageView
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
