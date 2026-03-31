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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Bundle
import android.os.Message
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.callMethod
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.chainMethod
import java.io.File
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MoreBatteryInfoNew : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        manufacturingDateMethod
        firstUsageDateMethod
        fullCapacityMethod
        designCapacityMethod

        return true
    }

    private val manufacturingDateMethod by lazy<Method> {
        requiredMember("manufacturingDate") {
            it.findMethod {
                matcher {
                    addUsingString("manufacturing_date")
                }
            }.single()
        }
    }

    private val firstUsageDateMethod by lazy<Method> {
        requiredMember("firstUsageDate") {
            it.findMethod {
                matcher {
                    addUsingString("first_usage_date")
                }
            }.single()
        }
    }

    private val fullCapacityMethod by lazy<Method> {
        requiredMember("chargeFull") {
            it.findMethod {
                matcher {
                    addUsingString("getBatteryChargeFull")
                }
            }.single()
        }
    }

    private val designCapacityMethod by lazy<Method> {
        requiredMember("chargeFullDesign") {
            it.findMethod {
                matcher {
                    addUsingString("charge_full_design")
                }
            }.single()
        }
    }

    private const val CHARGE_PROTECT_FRAGMENT_CLASS =
        "com.miui.powercenter.nightcharge.ChargeProtectFragment"
    private const val CHARGE_PROTECT_HANDLER_CLASS =
        $$"com.miui.powercenter.nightcharge.ChargeProtectFragment$d"

    private const val BATTERY_HEALTH_KEY = "reference_battery_health"
    private const val CURRENT_TEMP_KEY = "reference_current_temp"
    private const val TODAY_CHARGE_KEY = "reference_toady_charge_time"
    private const val CYCLE_COUNT_KEY = "reference_cycle_count"
    private const val PRODUCTION_DATE_KEY = "reference_production_date"
    private const val FIRST_USE_DATE_KEY = "reference_first_use_date"
    private const val DESIGN_CAPACITY_KEY = "reference_battery_design_capacity"
    private const val FULL_CAPACITY_KEY = "reference_battery_full_capacity"
    private const val CAPACITY_EMPTY_TEXT = "--"
    private const val BATTERY_DATE_FORMAT = "yyyyMMdd"
    private const val BATTERY_PROPERTY_MANUFACTURING_DATE = 7
    private const val BATTERY_PROPERTY_FIRST_USAGE_DATE = 8
    private const val BATTERY_SYSFS_DIR = "/sys/class/power_supply/battery"
    private const val BATTERY_BMS_SYSFS_DIR = "/sys/class/power_supply/bms"

    private val keepPreferenceKeys = setOf(
        BATTERY_HEALTH_KEY,
        CURRENT_TEMP_KEY,
        TODAY_CHARGE_KEY,
        CYCLE_COUNT_KEY,
        PRODUCTION_DATE_KEY,
        FIRST_USE_DATE_KEY,
        DESIGN_CAPACITY_KEY,
        FULL_CAPACITY_KEY
    )

    private var batteryInfoCategory: Any? = null
    private var productionDatePreference: Any? = null
    private var firstUseDatePreference: Any? = null
    private var designCapacityPreference: Any? = null
    private var fullCapacityPreference: Any? = null
    @SuppressLint("StaticFieldLeak")
    private var batteryContext: Context? = null

    override fun init() {
        hookPreferenceRemoval()
        hookChargeProtectFragment()
        hookChargeProtectHandler()
    }

    private fun hookPreferenceRemoval() {
        val preferenceGroupClass = findClassIfExists("androidx.preference.PreferenceGroup")
        val preferenceClass = findClassIfExists("androidx.preference.Preference")
        if (preferenceGroupClass == null || preferenceClass == null) return

        runCatching {
            preferenceGroupClass.chainMethod(
                "removePreference",
                preferenceClass
            ) {
                val preference = getArg(0)
                val key = runCatching {
                    preference?.callMethod("getKey") as? String
                }.getOrNull()

                if (key != null && key in keepPreferenceKeys) {
                    return@chainMethod false
                }

                proceed()
            }
        }.onFailure {
            XposedLog.e(TAG, packageName, "Failed to hook PreferenceGroup.removePreference(Preference)", it)
        }
    }

    private fun hookChargeProtectFragment() {
        val fragmentClass = findClassIfExists(CHARGE_PROTECT_FRAGMENT_CLASS) ?: return

        runCatching {
            fragmentClass.chainMethod("onCreatePreferences", Bundle::class.java, String::class.java) {
                val result = proceed()
                captureBatteryPreferences(thisObject)
                refreshDynamicBatteryRows()
                result
            }
        }.onFailure {
            XposedLog.e(TAG,
                packageName, "Failed to hook $CHARGE_PROTECT_FRAGMENT_CLASS#onCreatePreferences", it)
        }
    }

    private fun hookChargeProtectHandler() {
        val handlerClass = findClassIfExists(CHARGE_PROTECT_HANDLER_CLASS) ?: return

        runCatching {
            handlerClass.chainMethod("handleMessage", Message::class.java) {
                val result = proceed()
                refreshChargeProtectRows(thisObject)
                refreshDynamicBatteryRows()
                result
            }
        }.onFailure {
            XposedLog.e(TAG,
                packageName, "Failed to hook $CHARGE_PROTECT_HANDLER_CLASS#handleMessage", it)
        }
    }

    private fun captureBatteryPreferences(fragment: Any?) {
        if (fragment == null) return

        batteryContext = resolveFragmentContext(fragment)
        val findPreferenceByKey: (String) -> Any? = { key ->
            runCatching {
                fragment.callMethod("findPreference", key)
            }.getOrNull()
        }

        batteryInfoCategory = findPreferenceByKey("preference_key_category_battery_info")
        productionDatePreference = findPreferenceByKey(PRODUCTION_DATE_KEY)
        firstUseDatePreference = findPreferenceByKey(FIRST_USE_DATE_KEY)
    }

    private fun refreshChargeProtectRows(handler: Any?) {
        if (handler == null) return

        runCatching {
            handler.callMethod("a")
        }.onFailure {
            XposedLog.e(TAG, packageName, "Refresh callback failed: a()", it)
        }

        runCatching {
            handler.callMethod("b")
        }.onFailure {
            XposedLog.e(TAG, packageName, "Refresh callback failed: b()", it)
        }
    }

    private fun ensureDynamicBatteryPreferences(modRes: Resources) {
        val category = batteryInfoCategory ?: return
        val context = batteryContext ?: return
        val needDesignCapacity = designCapacityPreference == null
        val needFullCapacity = fullCapacityPreference == null

        if (!needDesignCapacity && !needFullCapacity) {
            return
        }

        val nextOrder = resolveNextPreferenceOrder(category)

        if (needDesignCapacity) {
            designCapacityPreference = ensureTextPreference(
                category,
                context,
                DESIGN_CAPACITY_KEY,
                modRes.getString(R.string.security_center_battery_design_capacity),
                nextOrder
            )
        }
        if (needFullCapacity) {
            fullCapacityPreference = ensureTextPreference(
                category,
                context,
                FULL_CAPACITY_KEY,
                modRes.getString(R.string.security_center_battery_full_capacity),
                nextOrder + 1
            )
        }
    }

    private fun refreshDynamicBatteryRows() {
        val context = batteryContext ?: return
        val modRes = AppsTool.getModuleRes(context)

        ensureDynamicBatteryPreferences(modRes)

        updateDatePreference(
            productionDatePreference,
            resolveBatteryDateText(
                officialMethod = manufacturingDateMethod,
                fallbackProperty = BATTERY_PROPERTY_MANUFACTURING_DATE,
                context = context,
                label = "production"
            )
        )

        updateDatePreference(
            firstUseDatePreference,
            resolveBatteryDateText(
                officialMethod = firstUsageDateMethod,
                fallbackProperty = BATTERY_PROPERTY_FIRST_USAGE_DATE,
                context = context,
                label = "first use"
            )
        )

        setPreferenceText(
            designCapacityPreference,
            resolveCapacityText(
                designCapacityMethod,
                modRes,
                listOf(
                    "$BATTERY_SYSFS_DIR/charge_full_design",
                    "$BATTERY_BMS_SYSFS_DIR/charge_full_design"
                )
            )
        )

        setPreferenceText(
            fullCapacityPreference,
            resolveCapacityText(
                fullCapacityMethod,
                modRes,
                listOf(
                    "$BATTERY_SYSFS_DIR/charge_full",
                    "$BATTERY_BMS_SYSFS_DIR/charge_full"
                )
            )
        )
    }

    private fun ensureTextPreference(
        category: Any,
        context: Context,
        key: String,
        title: String,
        order: Int
    ): Any? {
        val textPreferenceClass =
            findClassIfExists("miuix.preference.TextPreference") ?: return null

        val firstAttempt = runCatching {
            EzxHelpUtils.newInstance(textPreferenceClass, context)
        }
        val textPreference: Any? = firstAttempt.getOrNull() ?: run {
            runCatching {
                EzxHelpUtils.newInstance(textPreferenceClass, context, null)
            }.getOrNull()
        }

        val resolvedTextPreference = textPreference ?: run {
            XposedLog.e(TAG, packageName, "Unable to create TextPreference for $key")
            return null
        }

        runCatching { resolvedTextPreference.callMethod("setKey", key) }
        runCatching { resolvedTextPreference.callMethod("setTitle", title) }
        runCatching { resolvedTextPreference.callMethod("setClickable", false) }
        runCatching { resolvedTextPreference.callMethod("setTouchAnimationEnable", false) }
        runCatching { resolvedTextPreference.callMethod("setOrder", order) }

        runCatching {
            category.callMethod("addPreference", resolvedTextPreference)
        }.onFailure {
            XposedLog.e(TAG, packageName, "Failed to add dynamic preference $key", it)
        }
        return resolvedTextPreference
    }

    private fun resolveNextPreferenceOrder(category: Any): Int {
        val count = runCatching {
            category.callMethod("getPreferenceCount") as? Int
        }.getOrNull() ?: return 1000

        var maxOrder = Int.MIN_VALUE
        for (index in 0 until count) {
            val preference = runCatching {
                category.callMethod("getPreference", index)
            }.getOrNull() ?: continue
            val order = runCatching {
                preference.callMethod("getOrder") as? Int
            }.getOrNull() ?: continue
            if (order > maxOrder) {
                maxOrder = order
            }
        }

        if (maxOrder == Int.MIN_VALUE) {
            return count + 100
        }

        return if (maxOrder >= Int.MAX_VALUE - 2) {
            Int.MAX_VALUE - 2
        } else {
            maxOrder + 1
        }
    }

    private fun resolveBatteryDateText(
        officialMethod: Method,
        fallbackProperty: Int,
        context: Context,
        label: String
    ): String? {
        val officialCandidate = (callDexKitStaticValue(officialMethod) as? String)?.trim()
        val officialFormatted = formatBatteryDateCandidate(officialCandidate)
        if (!officialFormatted.isNullOrBlank()) return officialFormatted

        val fallbackSeconds = readBatteryManagerDateSeconds(context, fallbackProperty)
        val fallbackCandidate = fallbackSeconds?.let { secondsToYyyyMMdd(it) }
        val fallbackFormatted = formatBatteryDateCandidate(fallbackCandidate)
        if (!fallbackFormatted.isNullOrBlank()) return fallbackFormatted

        val rawSysfsCandidate = readDateFromSysfs(label)
        return formatBatteryDateCandidate(rawSysfsCandidate)
    }

    private fun formatBatteryDateCandidate(candidate: String?): String? {
        val value = candidate?.trim()
        if (!isValidDateDigits(value)) {
            return null
        }

        return runCatching {
            val parser = SimpleDateFormat(BATTERY_DATE_FORMAT, Locale.US).apply {
                isLenient = false
            }
            val parsed = parser.parse(value) ?: return@runCatching null
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                "yMMM"
            )
            SimpleDateFormat(pattern, Locale.getDefault()).format(parsed)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun isValidDateDigits(value: String?): Boolean {
        return value != null &&
            value.length == 8 &&
            value.all { it in '0'..'9' } &&
            value != "00000000" &&
            value != "99999999"
    }

    private fun readBatteryManagerDateSeconds(context: Context, propertyId: Int): Long? {
        return runCatching {
            val batteryManager = context.getSystemService(BatteryManager::class.java)
                ?: return@runCatching null
            val value = batteryManager.getLongProperty(propertyId)
            if (value > 0) value else null
        }.getOrNull()
    }

    private fun secondsToYyyyMMdd(seconds: Long): String? {
        if (seconds <= 0) {
            return null
        }
        return runCatching {
            SimpleDateFormat(BATTERY_DATE_FORMAT, Locale.US).format(Date(seconds * 1000L))
        }.getOrNull()
    }

    private fun resolveCapacityText(
        method: Method,
        modRes: Resources,
        sysfsPaths: List<String>
    ): String {
        val rawCapacity = when (val value = callDexKitStaticValue(method)) {
            is Int -> value.toLong()
            is Long -> value
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        } ?: readFirstPositiveNumber(sysfsPaths)?.toLong()

        return if (rawCapacity != null && rawCapacity > 0) {
            modRes.getString(
                R.string.security_center_battery_capacity_format,
                rawCapacity / 1000L
            )
        } else {
            CAPACITY_EMPTY_TEXT
        }
    }

    private fun setPreferenceText(preference: Any?, text: String) {
        if (preference == null) {
            return
        }

        runCatching {
            preference.callMethod("setText", text)
        }.onFailure {
            XposedLog.e(TAG, packageName, "Failed to set text for ${preference.javaClass.name}", it)
        }
    }

    private fun updateDatePreference(preference: Any?, text: String?) {
        if (preference == null) {
            return
        }

        runCatching {
            preference.callMethod("setVisible", !text.isNullOrBlank())
        }.onFailure {
            XposedLog.e(
                TAG,
                packageName,
                "Failed to set visibility=${!text.isNullOrBlank()} for ${preference.javaClass.name}",
                it
            )
        }
        text?.let { setPreferenceText(preference, it) }
    }

    private fun resolveFragmentContext(fragment: Any?): Context? {
        if (fragment == null) {
            return null
        }

        val context = runCatching {
            fragment.callMethod("getContext") as? Context
        }.getOrNull()
        if (context != null) {
            return context
        }

        return runCatching {
            fragment.callMethod("requireContext") as? Context
        }.getOrNull()
    }

    private fun callDexKitStaticValue(method: Method): Any? {
        return runCatching {
            method.isAccessible = true
            method.invoke(null)
        }.getOrNull()
    }

    private fun readDateFromSysfs(label: String): String? {
        val candidates = when (label) {
            "production" -> listOf(
                "$BATTERY_SYSFS_DIR/manufacturing_date",
                "$BATTERY_SYSFS_DIR/production_date",
                "$BATTERY_BMS_SYSFS_DIR/manufacturing_date"
            )
            "first use" -> listOf(
                "$BATTERY_SYSFS_DIR/first_usage_date",
                "$BATTERY_BMS_SYSFS_DIR/first_usage_date"
            )
            else -> emptyList()
        }

        for (path in candidates) {
            val value = readFirstLine(path)
            if (!value.isNullOrBlank()) {
                return value.trim()
            }
        }
        return null
    }

    private fun readFirstPositiveNumber(paths: List<String>): Int? {
        for (path in paths) {
            val raw = readFirstLine(path)?.trim().orEmpty()
            val value = raw.toIntOrNull()
            if (value != null && value > 0) {
                return value
            }
        }
        return null
    }

    private fun readFirstLine(path: String): String? {
        return runCatching {
            val file = File(path)
            if (!file.exists()) {
                return@runCatching null
            }
            file.bufferedReader().use { reader ->
                reader.readLine()
            }
        }.getOrNull()
    }

}
