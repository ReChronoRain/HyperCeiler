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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui

import android.content.Context
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.sevtinge.hyperceiler.libhook.base.BaseHook

/**
 * 读取某个物理卡槽当前注册网络的真实名称。
 *
 * 取数顺序（任何一步拿不到就继续回退，全部失败返回 null 表示保留原文字）：
 * 1. ServiceState -> getNetworkRegistrationInfo(PS 再 CS) -> CellIdentity -> getOperatorAlphaLong / getOperatorAlphaShort
 *    即 ROM 注册层写入的 alpha 名称，与控制中心"应该"显示的名称最接近，但依赖隐藏 API 反射。
 * 2. ServiceState.getOperatorAlphaLong / getOperatorAlphaShort 公开 API。
 * 3. TelephonyManager.getNetworkOperatorName（网络名），再 getSimOperatorName（网络名不可用时的兜底）。
 * 4. SubscriptionInfo.getCarrierName（SIM 卡内置运营商名），仅在没有任何网络注册信息时使用。
 *
 * 注意：卡槽必须只对应一个有效订阅。同一卡槽出现多条订阅时无法判断数据归属，直接返回 null，
 * 不做任何猜测。结果会按 (slot, 网络代码, 卡内代码, 语音/数据注册状态) 缓存，避免每条回调都反射。
 */
object CarrierNetworkName {
    private const val DOMAIN_CS = 1
    private const val DOMAIN_PS = 2
    private const val TRANSPORT_WWAN = 1
    private const val STATE_IN_SERVICE = 0
    private const val CACHE_ENTRY_MILLIS = 60_000L
    private const val CACHE_MAX_ENTRIES = 8

    private class NameSource(val name: String, val priority: Int)

    private class CacheEntry(val key: String, val at: Long, val name: String?)

    private val cache = object : LinkedHashMap<String, CacheEntry>(CACHE_MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
            size > CACHE_MAX_ENTRIES
    }

    /**
     * @param slot 物理卡槽（0 或 1）
     * @return 真实网络名；读不到时返回 null，调用方应保留原始文字
     */
    fun resolve(context: Context?, slot: Int): String? {
        if (context == null || slot !in 0..1) return null
        return try {
            resolveUncached(context, slot)
        } catch (error: Exception) {
            null
        } catch (error: LinkageError) {
            null
        }
    }

    // 这里需要吞掉任何异常：取数失败必须保持"保留原文"的原语义，不能影响系统界面渲染。
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private fun resolveUncached(context: Context, slot: Int): String? {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            ?: return null
        val subscriptions = subscriptionManager.activeSubscriptionInfoList
            ?.filter { it.simSlotIndex == slot }
            .orEmpty()
        // 0 条：该卡槽没有 SIM；多于 1 条：数据归属不唯一。两种情况都不替换。
        if (subscriptions.size != 1) return null
        val subscription = subscriptions.first()
        val subscriptionId = subscription.subscriptionId
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
            ?.createForSubscriptionId(subscriptionId)
            ?: return null

        val serviceState = try {
            telephonyManager.serviceState
        } catch (error: SecurityException) {
            null
        }
        val networkOperator = runCatching { telephonyManager.networkOperator }.getOrNull().orEmpty()
        val simOperator = runCatching { telephonyManager.simOperator }.getOrNull().orEmpty()
        val key = listOf(
            slot,
            networkOperator,
            simOperator,
            serviceState?.state ?: -1,
            // 隐藏 API，只能反射读取；读不到就当作未知，只影响缓存命中。
            readInt(serviceState, "getDataRegState") ?: -1
        ).joinToString("|")

        synchronized(cache) {
            val hit = cache[key]
            if (hit != null && System.currentTimeMillis() - hit.at < CACHE_ENTRY_MILLIS) return hit.name
        }

        val name = firstUsableName(collect(serviceState, telephonyManager, subscription))
        synchronized(cache) {
            cache[key] = CacheEntry(key, System.currentTimeMillis(), name)
        }
        return name
    }

    /**
     * 清空缓存；仅供回归测试在用例之间重置状态。
     */
    @Suppress("unused")
    internal fun clearCache() {
        synchronized(cache) { cache.clear() }
    }

    private fun collect(
        serviceState: ServiceState?,
        telephonyManager: TelephonyManager,
        subscription: android.telephony.SubscriptionInfo
    ): List<NameSource> = buildList {
        if (serviceState != null) {
            // 注册层名称（优先级 1）依赖隐藏 API，反射失败时 ServiceState 仍可能给出过期/占位文字，
            // 所以只有能确认"已注册"时才收：优先看数据/语音注册状态，读不到再看公开 alpha 是否非空。
            val registered = isRegistered(serviceState)
            if (registered) addAll(registrationAlphaNames(serviceState))
            readString(serviceState, "getOperatorAlphaLong")?.let { add(NameSource(it, if (registered) 2 else 3)) }
            readString(serviceState, "getOperatorAlphaShort")?.let { add(NameSource(it, if (registered) 2 else 3)) }
        }
        readString(telephonyManager, "getNetworkOperatorName")?.let { add(NameSource(it, 3)) }
        readString(telephonyManager, "getSimOperatorName")?.let { add(NameSource(it, 4)) }
        readString(subscription, "getCarrierName")?.let { add(NameSource(it, 5)) }
    }

    /**
     * 是否已注册到网络（语音或数据任一域在服务中）。
     * 两个状态都读不到时退化为"公开 alpha 名称非空"，与 UE Logo 原实现的判断保持一致。
     */
    private fun isRegistered(serviceState: ServiceState): Boolean {
        val voice = readInt(serviceState, "getState")
        val data = readInt(serviceState, "getDataRegState")
        if (voice == null && data == null) {
            return !readString(serviceState, "getOperatorAlphaLong").isNullOrEmpty() ||
                !readString(serviceState, "getOperatorAlphaShort").isNullOrEmpty()
        }
        return voice == STATE_IN_SERVICE || data == STATE_IN_SERVICE
    }

    /**
     * 数据域（PS）是否可信：只有数据注册状态明确在服务中才读 PS 的注册名称。
     * getDataRegState 是隐藏 API，反射失败时读不到状态，此时 PS 的名称可能是过期值，不能采用。
     */
    private fun dataRegistered(serviceState: ServiceState): Boolean =
        readInt(serviceState, "getDataRegState") == STATE_IN_SERVICE

    /** ROM 注册层名称：先数据域（PS）再语音域（CS），与控制中心实际展示顺序一致。 */
    private fun registrationAlphaNames(serviceState: ServiceState): List<NameSource> = buildList {
        val dataOk = dataRegistered(serviceState)
        for (domain in intArrayOf(DOMAIN_PS, DOMAIN_CS)) {
            // PS 名称只在数据注册状态可信时采用，避免反射失败后读到过期值。
            if (domain == DOMAIN_PS && !dataOk) continue
            val registration = invoke(serviceState, "getNetworkRegistrationInfo", domain, TRANSPORT_WWAN) ?: continue
            val cellIdentity = invoke(registration, "getCellIdentity") ?: continue
            readString(cellIdentity, "getOperatorAlphaLong")?.let { add(NameSource(it, 1)) }
            readString(cellIdentity, "getOperatorAlphaShort")?.let { add(NameSource(it, 1)) }
        }
    }

    /**
     * 按优先级挑第一个可用名称。收集阶段已保证元数据不一致的来源不会被收录，
     * 这里只负责跳过空值与字面量 "null"。
     */
    private fun firstUsableName(sources: List<NameSource>): String? =
        sources.asSequence().filter { usable(it.name) }.minByOrNull { it.priority }?.name

    private fun usable(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        return value.isNotEmpty() && "null" != value.lowercase()
    }

    private fun readString(target: Any, method: String): String? =
        invoke(target, method)?.toString()?.takeIf { it.isNotBlank() }

    private fun readInt(target: Any?, method: String): Int? =
        if (target == null) null else (invoke(target, method) as? Int)

    /**
     * 反射调用隐藏 API。查找与重载选择统一交给 [BaseHook.callMethod]（按实参类型择优，
     * 避免同名重载被静默选错），这里只负责吞掉异常：隐藏 API 可能因权限或版本直接抛错，
     * 单个来源失败必须降级，不能让整条解析链中断。
     */
    private fun invoke(target: Any, method: String, vararg args: Any?): Any? = try {
        BaseHook.callMethod(target, method, *args)
    } catch (error: Exception) {
        null
    } catch (error: LinkageError) {
        null
    }
}
