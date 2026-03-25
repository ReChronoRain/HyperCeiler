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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.createBitmap
import androidx.core.util.size
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.mobileSignalController
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MobileClass.networkController
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getIntField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethodAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.findViewByIdName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getBooleanField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIdByName
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getIntField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectField
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.hook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import java.util.concurrent.ConcurrentHashMap

class DualRowSignalHookV : MobileSignalHook() {
    private val ID_DUAL_CONTAINER by lazy { getOrCreateViewId("dual_signal_container") }
    private val ID_SIGNAL_SLOT1 by lazy { getOrCreateViewId("dual_signal_slot1") }
    private val ID_SIGNAL_SLOT2 by lazy { getOrCreateViewId("dual_signal_slot2") }

    private val rightMargin by lazy {
        PrefsBridge.getInt("system_ui_statusbar_mobile_network_icon_right_margin", 8) - 8
    }
    private val leftMargin by lazy {
        PrefsBridge.getInt("system_ui_statusbar_mobile_network_icon_left_margin", 8) - 8
    }
    private val iconScale by lazy {
        PrefsBridge.getInt("system_ui_statusbar_mobile_network_icon_size", 100)
    }
    private val verticalOffset by lazy {
        PrefsBridge.getInt("system_ui_statusbar_mobile_network_icon_vertical_offset", 40)
    }

    private val selectedIconStyle by lazy {
        PrefsBridge.getString("system_ui_status_mobile_network_icon_style", "")
    }

    private val dualSignalResMap = HashMap<String, Bitmap>(64)
    private val simSignalLevels = ConcurrentHashMap<Int, Int>()
    private val simDataSimState = ConcurrentHashMap<Int, Boolean>()
    private val simSlotIndices = ConcurrentHashMap<Int, Int>()

    @Volatile
    private var networkControllerInstance: Any? = null

    @Volatile
    private var dualSignalResLoaded = false

    private val viewDarkState = ConcurrentHashMap<Int, DarkInfo>()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun init() {
        listenMobileSignal()

        hookConstructAndBind { rootView, subId ->
            onViewCreated(rootView, subId)
        }

        hookDarkMode { rootView, darkInfo ->
            onDarkModeChanged(rootView, darkInfo)
        }
    }

    // ==================== 视图创建 ====================

    private fun onViewCreated(rootView: ViewGroup, subId: Int) {
        val mobileGroup = rootView.findById<LinearLayout>("mobile_group") ?: return
        val signalContainer: ViewGroup = rootView.findById("mobile_signal_container") ?: return
        val mobileSignal = rootView.findById<ImageView>("mobile_signal") ?: return

        val activeCount = getActiveMobileControllerCount()
        if (activeCount <= 1) {
            syncDualSignalVisibility(rootView, false)
            return
        }
        ensureDualSignalResLoaded(rootView.context)

        mobileGroup.setPadding(
            DisplayUtils.dp2px(leftMargin * 0.5f), 0,
            DisplayUtils.dp2px(rightMargin * 0.5f), 0
        )

        // 检查是否已有双排容器
        val existingContainer = rootView.findByViewId<FrameLayout>(ID_DUAL_CONTAINER)
        if (existingContainer != null) {
            cacheView(subId, rootView)
            syncDualSignalVisibility(rootView, true)
            if (simSignalLevels.isNotEmpty()) {
                val dark = viewDarkState[System.identityHashCode(rootView)]
                refreshDualIconsForView(rootView, dark?.isUseTint ?: false, dark?.isLight ?: true, dark?.color)
            }
            return
        }

        // 双卡模式：为每个 View 都创建双排信号容器
        val context = rootView.context
        val dualContainer = FrameLayout(context).apply {
            id = ID_DUAL_CONTAINER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val slot1 = ImageView(context).apply {
            id = ID_SIGNAL_SLOT1
            adjustViewBounds = true
        }
        val slot2 = ImageView(context).apply {
            id = ID_SIGNAL_SLOT2
            adjustViewBounds = true
        }

        val signalLp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (iconScale != 100) DisplayUtils.dp2px(iconScale / 10 * 2.0f) else ViewGroup.LayoutParams.MATCH_PARENT
        )
        dualContainer.addView(slot1, ViewGroup.LayoutParams(signalLp))
        dualContainer.addView(slot2, ViewGroup.LayoutParams(signalLp))

        signalContainer.addView(dualContainer)

        // 约束双排容器到 parent end（用 ID_DUAL_CONTAINER 作为约束锚点）
        val dualContainerId = ID_DUAL_CONTAINER

        // 约束双排容器到 parent end
        try {
            val dlp = dualContainer.layoutParams
            dlp.javaClass.getField("endToEnd").setInt(dlp, 0)
            dlp.javaClass.getField("topToTop").setInt(dlp, 0)
            dlp.javaClass.getField("bottomToBottom").setInt(dlp, 0)
            dualContainer.layoutParams = dlp
        } catch (_: Throwable) {
        }

        mobileSignal.visibility = View.GONE

        // OS3: ConstraintLayout 约束修正
        (signalContainer.findViewByIdName("mobile_type") as? ImageView)?.let { mobileType ->
            runCatching {
                val lp = mobileType.layoutParams
                lp.javaClass.getField("endToStart").setInt(lp, dualContainerId)
                lp.javaClass.getField("topToTop").setInt(lp, dualContainerId)
                mobileType.layoutParams = lp
            }
        }

        (signalContainer.findViewByIdName("mobile_left_mobile_inout") as? ImageView)?.let { inout ->
            runCatching {
                val lp = inout.layoutParams
                lp.javaClass.getField("endToStart").setInt(lp, dualContainerId)
                lp.javaClass.getField("bottomToBottom").setInt(lp, dualContainerId)
                lp.javaClass.getField("topToTop").setInt(lp, -1)
                inout.layoutParams = lp
            }
        }

        if (verticalOffset != 40) {
            dualContainer.translationY = DisplayUtils.dp2px((verticalOffset - 40) * 0.1f).toFloat()
        }

        cacheView(subId, rootView)
        syncDualSignalVisibility(rootView, true)

        if (simSignalLevels.isNotEmpty()) {
            refreshDualIconsForView(rootView, isUseTint = false, isLight = true)
        }

        setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_half_to_top_distance", 3f)
        setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_left_inout_over_strength", 0f)
        setDensityReplacement("com.android.systemui", "dimen", "status_bar_mobile_type_middle_to_strength_start", -0.4f)
    }

    // ==================== 反色处理 ====================

    private fun onDarkModeChanged(rootView: ViewGroup, darkInfo: DarkInfo) {
        val subId = try {
            getIntField(rootView, "subId")
        } catch (_: Throwable) { -1 }
        if (subId == -1 || simSignalLevels.isEmpty()) return

        viewDarkState[System.identityHashCode(rootView)] = darkInfo

        refreshDualIconsForView(rootView, darkInfo.isUseTint, darkInfo.isLight, darkInfo.color)
    }

    // ==================== 活跃 SIM 数量查询 ====================

    private fun getActiveMobileControllerCount(): Int {
        val controller = networkControllerInstance ?: return 0
        return try {
            controller.getObjectFieldAs<SparseArray<*>>("mMobileSignalControllers").size
        } catch (e: Throwable) {
            XposedLog.w(TAG, lpparam.packageName, "getActiveMobileControllerCount error: ${e.message}")
            0
        }
    }

    // ==================== 资源加载 ====================

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun ensureDualSignalResLoaded(context: Context) {
        if (dualSignalResLoaded) return
        synchronized(this) {
            if (dualSignalResLoaded) return

            val modRes = getModuleRes(context.applicationContext ?: context)
            dualSignalResMap.clear()

            val colorModes = if (selectedIconStyle == "theme") {
                arrayOf(
                    Triple("", false, true),
                    Triple("dark", false, false)
                )
            } else {
                arrayOf(
                    Triple("", false, true),
                    Triple("dark", false, false),
                    Triple("tint", true, true)
                )
            }

            for (slot in 1..2) {
                for (lvl in 0..5) {
                    for ((_, isUseTint, isLight) in colorModes) {
                        val resName = getSignalIconResName(slot, lvl, isUseTint, isLight)
                        val resId = modRes.getIdByName(resName, "drawable", ProjectApi.mAppModulePkg)
                        if (resId != 0) {
                            modRes.getDrawable(resId, null)?.let { drawable ->
                                dualSignalResMap[resName] = drawableToBitmap(drawable)
                            }
                        }
                    }
                }
            }

            dualSignalResLoaded = dualSignalResMap.isNotEmpty()
        }
    }

    private fun listenMobileSignal() {
        mobileSignalController.methodFinder()
            .filterByName("notifyListeners")
            .single()
            .hook {
                val result = proceed()
                val signalController = thisObject

                if (networkControllerInstance == null) {
                    runCatching {
                        networkControllerInstance =
                            signalController.getObjectField("mNetworkController")
                    }
                }

                val subscriptionInfo = signalController.getObjectFieldAs<Any>("mSubscriptionInfo")
                val subscriptionId = subscriptionInfo.callMethodAs<Int>("getSubscriptionId")
                val slotIndex = runCatching {
                    subscriptionInfo.callMethodAs<Int>("getSimSlotIndex")
                }.getOrElse {
                    SubscriptionManager.getSlotIndex(subscriptionId)
                }
                val currentState = signalController.getObjectFieldAs<Any>("mCurrentState")

                val dataSim = currentState.getBooleanField("dataSim")
                val connected = currentState.getBooleanField("connected")
                val enabled = runCatching {
                    currentState.getBooleanField("enabled")
                }.getOrDefault(true)
                val signalStrength = currentState.getObjectField("signalStrength")
                val rawLevel = runCatching {
                    currentState.getIntField("level")
                }.getOrElse {
                    if (!connected) 0 else signalStrength?.callMethodAs<Int>("getMiuiLevel") ?: 0
                }
                val mappedLevel = if (rawLevel >= 2) rawLevel + 1 else rawLevel
                val previousLevel = simSignalLevels[subscriptionId] ?: 0
                val level = if (!connected && enabled && mappedLevel == 0 && previousLevel > 0) {
                    previousLevel
                } else {
                    mappedLevel
                }
                val oldLevel = simSignalLevels.put(subscriptionId, level)
                val oldDataSim = simDataSimState.put(subscriptionId, dataSim)
                val oldSlotIndex = simSlotIndices.put(subscriptionId, slotIndex)

                if (oldLevel == level && oldDataSim == dataSim && oldSlotIndex == slotIndex) {
                    return@hook result
                }
                refreshAllCachedViews()
                result
            }

        networkController.methodFinder()
            .filterByName("setCurrentSubscriptionsLocked")
            .single()
            .hook {
                val networkCtrl = thisObject
                networkControllerInstance = networkCtrl

                val subList = args[0] as List<*>
                val currentSubscriptions = networkCtrl.getObjectFieldAs<List<*>>("mCurrentSubscriptions")
                val newSubIds = subList.filterNotNull().mapNotNull { subInfo ->
                    runCatching { subInfo.callMethodAs<Int>("getSubscriptionId") }.getOrNull()
                }.toSet()
                val newSlotIndices = subList.filterNotNull().associateNotNull { subInfo ->
                    val subId = runCatching { subInfo.callMethodAs<Int>("getSubscriptionId") }.getOrNull()
                    val slot = runCatching { subInfo.callMethodAs<Int>("getSimSlotIndex") }.getOrNull()
                        ?: subId?.let { SubscriptionManager.getSlotIndex(it) }
                    if (subId == null || slot == null || slot == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                        null
                    } else {
                        subId to slot
                    }
                }
                if (currentSubscriptions.size != subList.size) {
                    simSignalLevels.clear()
                    simDataSimState.clear()
                    simSlotIndices.clear()
                } else if (newSubIds.isNotEmpty()) {
                    val staleKeys = simSignalLevels.keys.filter { it !in newSubIds }
                    staleKeys.forEach { key ->
                        val staleLevel = simSignalLevels[key]
                        val staleSlot = simSlotIndices[key]
                        val replacementSubId = staleSlot?.let { slot ->
                            newSlotIndices.entries.firstOrNull { (subId, newSlot) ->
                                newSlot == slot && subId != key
                            }?.key
                        }
                        if (replacementSubId != null && staleLevel != null && staleLevel > 0) {
                            val currentReplacementLevel = simSignalLevels[replacementSubId] ?: 0
                            if (currentReplacementLevel <= 0) {
                                simSignalLevels[replacementSubId] = staleLevel
                                simDataSimState[replacementSubId] = simDataSimState[key] == true
                                staleSlot.let { simSlotIndices[replacementSubId] = it }
                            }
                        }
                        simSignalLevels.remove(key)
                        simDataSimState.remove(key)
                        simSlotIndices.remove(key)
                    }
                }

                val result = proceed()
                refreshAllCachedViews()
                result
            }
    }

    // ==================== 视图刷新 ====================

    private fun getSignalLevelsForRender(out: IntArray) {
        out[0] = 0
        out[1] = 0
        val orderedSubIds = simSignalLevels.keys.sortedWith(
            compareBy(
                {
                    when {
                        simDataSimState[it] == true -> 0
                        it == SubscriptionManager.getDefaultDataSubscriptionId() -> 1
                        else -> 2
                    }
                },
                { simSlotIndices[it] ?: Int.MAX_VALUE },
                { it }
            )
        )
        orderedSubIds.getOrNull(0)?.let { out[0] = simSignalLevels[it] ?: 0 }
        orderedSubIds.getOrNull(1)?.let { out[1] = simSignalLevels[it] ?: 0 }
    }

    private val renderLevels = IntArray(2)

    private fun refreshDualIconsForView(
        rootView: ViewGroup,
        isUseTint: Boolean,
        isLight: Boolean,
        color: Int? = null
    ) {
        val shouldUseDual = getActiveMobileControllerCount() > 1
        syncDualSignalVisibility(rootView, shouldUseDual)
        if (!shouldUseDual) return

        val dualContainer = rootView.findByViewId<FrameLayout>(ID_DUAL_CONTAINER) ?: return
        if (dualContainer.visibility != View.VISIBLE) return

        val slot1 = dualContainer.findByViewId<ImageView>(ID_SIGNAL_SLOT1) ?: return
        val slot2 = dualContainer.findByViewId<ImageView>(ID_SIGNAL_SLOT2) ?: return

        getSignalLevelsForRender(renderLevels)
        val dataLevel = renderLevels[0]
        val noDataLevel = renderLevels[1]

        val slot1ResName = getSignalIconResName(1, dataLevel, isUseTint, isLight)
        val slot2ResName = getSignalIconResName(2, noDataLevel, isUseTint, isLight)
        val slot1Bitmap = dualSignalResMap[slot1ResName]
        val slot2Bitmap = dualSignalResMap[slot2ResName]
        if (slot1Bitmap == null || slot2Bitmap == null) {
            XposedLog.w(
                TAG,
                lpparam.packageName,
                "refreshDualIcons: bitmap not found! slot1=$slot1ResName, slot2=$slot2ResName"
            )
            return
        }

        val needsTint = isUseTint && selectedIconStyle != "theme"
        slot1.setImageBitmap(slot1Bitmap)
        slot2.setImageBitmap(slot2Bitmap)

        if (needsTint && color != null) {
            slot1.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            slot2.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else {
            slot1.clearColorFilter()
            slot2.clearColorFilter()
        }
    }

    private val refreshRunnable = Runnable {
        viewCache.values.forEach { viewSet ->
            val iter = viewSet.iterator()
            while (iter.hasNext()) {
                val rootView = iter.next()
                if (!rootView.isAttachedToWindow) {
                    iter.remove()
                    continue
                }
                val dark = viewDarkState[System.identityHashCode(rootView)]
                refreshDualIconsForView(rootView, dark?.isUseTint ?: false, dark?.isLight ?: true, dark?.color)
            }
        }
    }

    private fun refreshAllCachedViews() {
        mainHandler.removeCallbacks(refreshRunnable)
        mainHandler.post(refreshRunnable)
    }

    private fun syncDualSignalVisibility(rootView: ViewGroup, useDualSignal: Boolean) {
        val dualContainer = rootView.findByViewId<FrameLayout>(ID_DUAL_CONTAINER)
        val mobileSignal = rootView.findById<ImageView>("mobile_signal")
        dualContainer?.visibility = if (useDualSignal) View.VISIBLE else View.GONE
        mobileSignal?.visibility = if (useDualSignal) View.GONE else View.VISIBLE
    }

    private inline fun <T, R> Iterable<T>.associateNotNull(transform: (T) -> Pair<R, Int>?): Map<R, Int> {
        val out = LinkedHashMap<R, Int>()
        for (item in this) {
            val pair = transform(item) ?: continue
            out[pair.first] = pair.second
        }
        return out
    }

    // ==================== 图标资源名称生成 ====================

    private fun getSignalIconResName(slot: Int, level: Int, isUseTint: Boolean, isLight: Boolean): String {
        val iconStyle = if (selectedIconStyle.isNotEmpty()) "_$selectedIconStyle" else ""
        val colorMode = if (!isUseTint || selectedIconStyle == "theme") {
            if (!isLight) "_dark" else ""
        } else {
            "_tint"
        }
        return "statusbar_signal_${slot}_$level$colorMode$iconStyle"
    }
}
