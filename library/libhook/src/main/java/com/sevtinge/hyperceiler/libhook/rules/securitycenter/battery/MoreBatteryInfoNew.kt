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
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Bundle
import android.os.Message
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.interceptHookMethod
import java.io.File
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object MoreBatteryInfoNew : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        manufacturingDateMethod.apply { isAccessible = true }
        firstUsageDateMethod.apply { isAccessible = true }
        fullCapacityMethod.apply { isAccessible = true }
        designCapacityMethod.apply { isAccessible = true }

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
    private const val MAX_CAPACITY_KEY = "reference_battery_max_capacity"
    private const val VOLTAGE_KEY = "reference_battery_voltage"
    private const val TECHNOLOGY_KEY = "reference_battery_technology"
    private const val STATUS_KEY = "reference_battery_status"
    private const val CURRENT_NOW_KEY = "reference_battery_current_now"
    private const val CAPACITY_LEVEL_KEY = "reference_battery_capacity_level"
    private const val TIME_TO_FULL_KEY = "reference_battery_time_to_full"

    private const val CAPACITY_EMPTY_TEXT = "--"
    private const val BATTERY_DATE_FORMAT = "yyyyMMdd"
    private const val BATTERY_PROPERTY_MANUFACTURING_DATE = 7
    private const val BATTERY_PROPERTY_FIRST_USAGE_DATE = 8
    private const val BATTERY_SYSFS_DIR = "/sys/class/power_supply/battery"
    private const val BATTERY_BMS_SYSFS_DIR = "/sys/class/power_supply/bms"

    private val DESIGN_CAPACITY_PATHS = listOf(
        "$BATTERY_SYSFS_DIR/charge_full_design",
        "$BATTERY_BMS_SYSFS_DIR/charge_full_design"
    )
    private val FULL_CAPACITY_PATHS = listOf(
        "$BATTERY_SYSFS_DIR/charge_full",
        "$BATTERY_BMS_SYSFS_DIR/charge_full"
    )
    private val PRODUCTION_DATE_PATHS = listOf(
        "$BATTERY_SYSFS_DIR/manufacturing_date",
        "$BATTERY_SYSFS_DIR/production_date",
        "$BATTERY_BMS_SYSFS_DIR/manufacturing_date"
    )
    private val FIRST_USE_DATE_PATHS = listOf(
        "$BATTERY_SYSFS_DIR/first_usage_date",
        "$BATTERY_BMS_SYSFS_DIR/first_usage_date"
    )
    private val CYCLE_COUNT_PATHS = listOf(
        "$BATTERY_SYSFS_DIR/cycle_count",
        "/sys/class/power_supply/mtk-gauge/cycle_count",
        "$BATTERY_SYSFS_DIR/battery_cycle",
        "/sys/class/power_supply/mtk-gauge/battery_cycle",
        "$BATTERY_BMS_SYSFS_DIR/cycle_count",
        "$BATTERY_BMS_SYSFS_DIR/battery_cycle",
        "$BATTERY_SYSFS_DIR/cycles",
        "$BATTERY_SYSFS_DIR/device/cycle_count"
    )

    private val keepPreferenceKeys = setOf(
        BATTERY_HEALTH_KEY,
        CURRENT_TEMP_KEY,
        TODAY_CHARGE_KEY,
        CYCLE_COUNT_KEY,
        PRODUCTION_DATE_KEY,
        FIRST_USE_DATE_KEY,
        DESIGN_CAPACITY_KEY,
        FULL_CAPACITY_KEY,
        MAX_CAPACITY_KEY,
        VOLTAGE_KEY,
        TECHNOLOGY_KEY,
        STATUS_KEY,
        CURRENT_NOW_KEY,
        CAPACITY_LEVEL_KEY,
        TIME_TO_FULL_KEY
    )

    private var batteryInfoCategory: Any? = null
    private var batteryHealthPreference: Any? = null
    private var productionDatePreference: Any? = null
    private var firstUseDatePreference: Any? = null
    private var designCapacityPreference: Any? = null
    private var fullCapacityPreference: Any? = null
    private var maxCapacityPreference: Any? = null
    private var voltagePreference: Any? = null
    private var technologyPreference: Any? = null
    private var statusPreference: Any? = null
    private var currentNowPreference: Any? = null
    private var capacityLevelPreference: Any? = null
    private var timeToFullPreference: Any? = null
    private var cycleCountPreference: Any? = null
    private var todayChargePreference: Any? = null
    private var currentTempPreference: Any? = null

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
            preferenceGroupClass.interceptHookMethod(
                "removePreference",
                preferenceClass
            ) { chain ->
                val preference = chain.getArg(0)
                val key = runCatching {
                    preference?.callMethod("getKey") as? String
                }.getOrNull()
                if (key != null && key in keepPreferenceKeys) {
                    return@interceptHookMethod false
                }

                chain.proceed()
            }
        }.onFailure {
            XposedLog.e(TAG, packageName, "Failed to hook PreferenceGroup.removePreference(Preference)", it)
        }
    }

    private fun hookChargeProtectFragment() {
        val fragmentClass = findClassIfExists(CHARGE_PROTECT_FRAGMENT_CLASS) ?: return

        runCatching {
            fragmentClass.interceptHookMethod("onCreatePreferences", Bundle::class.java, String::class.java) { chain ->
                clearCapturedPreferences()
                val result = chain.proceed()
                captureBatteryPreferences(chain.thisObject)
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
            handlerClass.interceptHookMethod("handleMessage", Message::class.java) { chain ->
                val result = chain.proceed()
                val handler = chain.thisObject ?: return@interceptHookMethod result
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
                refreshDynamicBatteryRows()
                result
            }
        }.onFailure {
            XposedLog.e(TAG,
                packageName, "Failed to hook $CHARGE_PROTECT_HANDLER_CLASS#handleMessage", it)
        }
    }

    private fun captureBatteryPreferences(fragment: Any?) {
        if (fragment == null) {
            clearCapturedPreferences()
            return
        }

        batteryContext = runCatching {
            fragment.callMethod("getContext") as? Context
        }.getOrNull() ?: runCatching {
            fragment.callMethod("requireContext") as? Context
        }.getOrNull()
        val findPreferenceByKey: (String) -> Any? = { key ->
            runCatching {
                fragment.callMethod("findPreference", key)
            }.getOrNull()
        }

        batteryInfoCategory = findPreferenceByKey("preference_key_category_battery_info")
        batteryHealthPreference = findPreferenceByKey(BATTERY_HEALTH_KEY)
        productionDatePreference = findPreferenceByKey(PRODUCTION_DATE_KEY)
        firstUseDatePreference = findPreferenceByKey(FIRST_USE_DATE_KEY)
        cycleCountPreference = findPreferenceByKey(CYCLE_COUNT_KEY)
        todayChargePreference = findPreferenceByKey(TODAY_CHARGE_KEY)
        currentTempPreference = findPreferenceByKey(CURRENT_TEMP_KEY)
        designCapacityPreference = findPreferenceByKey(DESIGN_CAPACITY_KEY)
        fullCapacityPreference = findPreferenceByKey(FULL_CAPACITY_KEY)
        maxCapacityPreference = findPreferenceByKey(MAX_CAPACITY_KEY)
        voltagePreference = findPreferenceByKey(VOLTAGE_KEY)
        technologyPreference = findPreferenceByKey(TECHNOLOGY_KEY)
        statusPreference = findPreferenceByKey(STATUS_KEY)
        currentNowPreference = findPreferenceByKey(CURRENT_NOW_KEY)
        capacityLevelPreference = findPreferenceByKey(CAPACITY_LEVEL_KEY)
        timeToFullPreference = findPreferenceByKey(TIME_TO_FULL_KEY)
    }

    private fun ensureDynamicBatteryPreferences(modRes: Resources) {
        val category = batteryInfoCategory ?: return
        val context = batteryContext ?: return

        if (designCapacityPreference == null) {
            designCapacityPreference = ensureTextPreference(
                category, context, DESIGN_CAPACITY_KEY,
                modRes.getString(R.string.security_center_battery_design_capacity), 10
            )
        } else {
            runCatching {
                designCapacityPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_design_capacity))
                designCapacityPreference?.callMethod("setOrder", 10)
            }
        }

        if (fullCapacityPreference == null) {
            fullCapacityPreference = ensureTextPreference(
                category, context, FULL_CAPACITY_KEY,
                modRes.getString(R.string.security_center_battery_full_capacity), 20
            )
        } else {
            runCatching {
                fullCapacityPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_full_capacity))
                fullCapacityPreference?.callMethod("setOrder", 20)
            }
        }

        if (maxCapacityPreference == null) {
            maxCapacityPreference = ensureTextPreference(
                category, context, MAX_CAPACITY_KEY,
                modRes.getString(R.string.security_center_battery_max_capacity), 30
            )
        } else {
            runCatching {
                maxCapacityPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_max_capacity))
                maxCapacityPreference?.callMethod("setOrder", 30)
            }
        }

        if (cycleCountPreference == null) {
            cycleCountPreference = ensureTextPreference(
                category, context, CYCLE_COUNT_KEY,
                modRes.getString(R.string.security_center_battery_cycle_count), 40
            )
        } else {
            runCatching {
                cycleCountPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_cycle_count))
                cycleCountPreference?.callMethod("setOrder", 40)
            }
        }

        if (statusPreference == null) {
            statusPreference = ensureTextPreference(
                category, context, STATUS_KEY,
                modRes.getString(R.string.security_center_battery_status), 50
            )
        } else {
            runCatching {
                statusPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_status))
                statusPreference?.callMethod("setOrder", 50)
            }
        }

        if (currentNowPreference == null) {
            currentNowPreference = ensureTextPreference(
                category, context, CURRENT_NOW_KEY,
                modRes.getString(R.string.security_center_battery_current_now), 60
            )
        } else {
            runCatching {
                currentNowPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_current_now))
                currentNowPreference?.callMethod("setOrder", 60)
            }
        }

        if (voltagePreference == null) {
            voltagePreference = ensureTextPreference(
                category, context, VOLTAGE_KEY,
                modRes.getString(R.string.security_center_battery_voltage), 70
            )
        } else {
            runCatching {
                voltagePreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_voltage))
                voltagePreference?.callMethod("setOrder", 70)
            }
        }

        currentTempPreference?.let { runCatching { it.callMethod("setOrder", 80) } }

        if (timeToFullPreference == null) {
            timeToFullPreference = ensureTextPreference(
                category, context, TIME_TO_FULL_KEY,
                modRes.getString(R.string.security_center_battery_time_to_full), 90
            )
        } else {
            runCatching {
                timeToFullPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_time_to_full))
                timeToFullPreference?.callMethod("setOrder", 90)
            }
        }

        if (capacityLevelPreference == null) {
            capacityLevelPreference = ensureTextPreference(
                category, context, CAPACITY_LEVEL_KEY,
                modRes.getString(R.string.security_center_battery_capacity_level), 100
            )
        } else {
            runCatching {
                capacityLevelPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_capacity_level))
                capacityLevelPreference?.callMethod("setOrder", 100)
            }
        }

        if (technologyPreference == null) {
            technologyPreference = ensureTextPreference(
                category, context, TECHNOLOGY_KEY,
                modRes.getString(R.string.security_center_battery_technology), 110
            )
        } else {
            runCatching {
                technologyPreference?.callMethod("setTitle", modRes.getString(R.string.security_center_battery_technology))
                technologyPreference?.callMethod("setOrder", 110)
            }
        }

        todayChargePreference?.let { runCatching { it.callMethod("setOrder", 120) } }
        firstUseDatePreference?.let { runCatching { it.callMethod("setOrder", 130) } }
        productionDatePreference?.let { runCatching { it.callMethod("setOrder", 140) } }
    }

    private fun refreshDynamicBatteryRows() {
        val context = batteryContext ?: return
        val modRes = AppsTool.getModuleRes(context)

        ensureDynamicBatteryPreferences(modRes)

        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val batteryManager = runCatching {
            context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        }.getOrNull()

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

        val rawDesignCapacity = readRawCapacity(designCapacityMethod, DESIGN_CAPACITY_PATHS)
        val rawFullCapacity = readRawCapacity(fullCapacityMethod, FULL_CAPACITY_PATHS)

        setPreferenceText(
            designCapacityPreference,
            formatCapacityText(rawDesignCapacity, modRes)
        )

        setPreferenceText(
            fullCapacityPreference,
            formatCapacityText(rawFullCapacity, modRes)
        )

        setPreferenceText(
            maxCapacityPreference,
            resolveMaxCapacityText(rawDesignCapacity, rawFullCapacity)
        )

        val cycleCountText = resolveCycleCountText(batteryIntent, batteryManager)
        setPreferenceText(cycleCountPreference, cycleCountText)

        updateBatteryHealthText(rawDesignCapacity, rawFullCapacity)

        setPreferenceText(
            voltagePreference,
            resolveVoltageText(batteryIntent)
        )

        setPreferenceText(
            technologyPreference,
            resolveTechnologyText(batteryIntent)
        )

        setPreferenceText(
            statusPreference,
            resolveStatusText(batteryIntent, modRes)
        )

        setPreferenceText(
            currentNowPreference,
            resolveCurrentNowText(batteryManager)
        )

        setPreferenceText(
            capacityLevelPreference,
            resolveCapacityLevelText(batteryIntent, modRes)
        )

        setPreferenceText(
            timeToFullPreference,
            resolveTimeToFullText(batteryManager)
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
            com.sevtinge.hyperceiler.libhook.base.BaseHook.newInstance(textPreferenceClass, context)
        }
        val textPreference: Any? = firstAttempt.getOrNull() ?: run {
            runCatching {
                com.sevtinge.hyperceiler.libhook.base.BaseHook.newInstance(textPreferenceClass, context, null)
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
        if (
            value == null ||
            value.length != 8 ||
            !value.all { it in '0'..'9' } ||
            value == "00000000" ||
            value == "99999999"
        ) {
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

    private fun readRawCapacity(
        method: Method,
        sysfsPaths: List<String>
    ): Long? {
        val rawCapacity = when (val value = callDexKitStaticValue(method)) {
            is Int -> value.toLong()
            is Long -> value
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        } ?: readFirstPositiveNumber(sysfsPaths)?.toLong()

        return if (rawCapacity != null && rawCapacity > 0) rawCapacity else null
    }

    private fun formatCapacityText(rawCapacity: Long?, modRes: Resources): String {
        return if (rawCapacity != null && rawCapacity > 0) {
            modRes.getString(
                R.string.security_center_battery_capacity_format,
                rawCapacity / 1000L
            )
        } else {
            CAPACITY_EMPTY_TEXT
        }
    }

    private fun resolveMaxCapacityText(rawDesign: Long?, rawFull: Long?): String {
        if (rawDesign != null && rawFull != null && rawDesign > 0 && rawFull > 0) {
            val ratio = (rawFull.toDouble() / rawDesign.toDouble()) * 100.0
            val percent = if (ratio > 100.0) 100.0 else ratio
            return String.format(Locale.US, "%.1f%%", percent)
        }
        val soh = readFirstLine("$BATTERY_SYSFS_DIR/soh")
            ?: readFirstLine("$BATTERY_BMS_SYSFS_DIR/soh")
        val sohInt = soh?.trim()?.toIntOrNull()
        if (sohInt != null && sohInt > 0) {
            return String.format(Locale.US, "%.1f%%", sohInt.toDouble())
        }
        return CAPACITY_EMPTY_TEXT
    }

    private fun resolveVoltageText(batteryIntent: Intent?): String {
        var voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        if (voltage <= 0) {
            voltage = readFirstPositiveNumber(listOf(
                "$BATTERY_SYSFS_DIR/voltage_now",
                "$BATTERY_BMS_SYSFS_DIR/voltage_now"
            ))?.let { if (it > 100000) it / 1000 else it } ?: -1
        }
        return if (voltage > 0) "$voltage mV" else CAPACITY_EMPTY_TEXT
    }

    private fun resolveTechnologyText(batteryIntent: Intent?): String {
        val tech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        if (!tech.isNullOrBlank()) {
            return tech
        }
        val sysfsTech = readFirstLine("$BATTERY_SYSFS_DIR/technology")
            ?: readFirstLine("$BATTERY_BMS_SYSFS_DIR/technology")
        return if (!sysfsTech.isNullOrBlank()) sysfsTech.trim() else "Li-poly"
    }

    private fun resolveStatusText(batteryIntent: Intent?, modRes: Resources): String {
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> modRes.getString(R.string.security_center_battery_status_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> modRes.getString(R.string.security_center_battery_status_discharging)
            BatteryManager.BATTERY_STATUS_FULL -> modRes.getString(R.string.security_center_battery_status_full)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> modRes.getString(R.string.security_center_battery_status_not_charging)
            else -> {
                val sysfsStatus = readFirstLine("$BATTERY_SYSFS_DIR/status")
                    ?: readFirstLine("$BATTERY_BMS_SYSFS_DIR/status")
                if (!sysfsStatus.isNullOrBlank()) sysfsStatus.trim() else "Discharging"
            }
        }
    }

    private fun resolveCurrentNowText(batteryManager: BatteryManager?): String {
        var current = runCatching {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }.getOrNull()
        if (current == null || current == 0 || current == Int.MIN_VALUE) {
            val raw = readFirstLine("$BATTERY_SYSFS_DIR/current_now")
                ?: readFirstLine("$BATTERY_BMS_SYSFS_DIR/current_now")
            current = raw?.trim()?.toIntOrNull()
        }
        if (current == null || current == 0) {
            return "0 mA"
        }
        val currentMa = if (abs(current) >= 10000) current / 1000 else current
        return "$currentMa mA"
    }

    private fun resolveCapacityLevelText(batteryIntent: Intent?, modRes: Resources): String {
        val sysfsLevel = readFirstLine("$BATTERY_SYSFS_DIR/capacity_level")
            ?: readFirstLine("$BATTERY_BMS_SYSFS_DIR/capacity_level")
        if (!sysfsLevel.isNullOrBlank()) {
            return sysfsLevel.trim()
        }
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> modRes.getString(R.string.security_center_battery_health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> modRes.getString(R.string.security_center_battery_health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> modRes.getString(R.string.security_center_battery_health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> modRes.getString(R.string.security_center_battery_health_over_voltage)
            BatteryManager.BATTERY_HEALTH_COLD -> modRes.getString(R.string.security_center_battery_health_cold)
            else -> "Normal"
        }
    }

    private fun resolveTimeToFullText(batteryManager: BatteryManager?): String {
        val remainingMs = runCatching {
            batteryManager?.computeChargeTimeRemaining()
        }.getOrNull() ?: -1L

        if (remainingMs > 0) {
            val minutes = remainingMs / 60000L
            return "$minutes min"
        }
        return "0 min"
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

    private fun callDexKitStaticValue(method: Method): Any? {
        return runCatching {
            method.invoke(null)
        }.getOrNull()
    }

    private fun readDateFromSysfs(label: String): String? {
        val candidates = when (label) {
            "production" -> PRODUCTION_DATE_PATHS
            "first use" -> FIRST_USE_DATE_PATHS
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

    private fun clearCapturedPreferences() {
        batteryInfoCategory = null
        batteryHealthPreference = null
        productionDatePreference = null
        firstUseDatePreference = null
        designCapacityPreference = null
        fullCapacityPreference = null
        maxCapacityPreference = null
        voltagePreference = null
        technologyPreference = null
        statusPreference = null
        currentNowPreference = null
        capacityLevelPreference = null
        timeToFullPreference = null
        cycleCountPreference = null
        todayChargePreference = null
        currentTempPreference = null
        batteryContext = null
    }

    private fun resolveCycleCountText(
        batteryIntent: Intent?,
        batteryManager: BatteryManager?
    ): String {
        val miChargeCycles = runCatching {
            val miChargeClass = findClassIfExists("miui.util.IMiCharge") ?: return@runCatching null
            val instance = miChargeClass.getMethod("getInstance").invoke(null) ?: return@runCatching null
            val raw = instance.javaClass.getMethod("getBatteryCycleCount").invoke(instance)
            when (raw) {
                is Int -> if (raw >= 0) raw else null
                is Number -> if (raw.toInt() >= 0) raw.toInt() else null
                is String -> raw.trim().toIntOrNull()?.takeIf { it >= 0 }
                null -> null
                else -> null
            }
        }.getOrNull()
        if (miChargeCycles != null) {
            return "$miChargeCycles"
        }

        val bmCycles = runCatching {
            batteryManager?.getIntProperty(10)
        }.getOrNull()?.takeIf { it >= 0 }
        if (bmCycles != null) {
            return "$bmCycles"
        }

        val intentCycles = runCatching {
            val c1 = batteryIntent?.getIntExtra("android.os.extra.CYCLE_COUNT", -1) ?: -1
            if (c1 >= 0) c1 else {
                val c2 = batteryIntent?.getIntExtra("cycle_count", -1) ?: -1
                if (c2 >= 0) c2 else null
            }
        }.getOrNull()
        if (intentCycles != null) {
            return "$intentCycles"
        }

        val sysfsCycles = readFirstCycleCount(CYCLE_COUNT_PATHS)
        if (sysfsCycles != null) {
            return "$sysfsCycles"
        }

        return CAPACITY_EMPTY_TEXT
    }

    private fun readFirstCycleCount(paths: List<String>): Int? {
        for (path in paths) {
            val raw = readFirstLine(path)?.trim().orEmpty()
            val value = raw.toIntOrNull()
            if (value != null && value >= 0) {
                return value
            }
        }
        return null
    }

    private fun updateBatteryHealthText(rawDesignCapacity: Long?, rawFullCapacity: Long?) {
        val pref = batteryHealthPreference ?: return
        val currentText = runCatching {
            pref.callMethod("getText")?.toString()?.trim()
        }.getOrNull()

        if (currentText.isNullOrBlank() || currentText.contains("null", ignoreCase = true) || currentText.contains(" %")) {
            val resolvedHealth = resolveHealthPercent(rawDesignCapacity, rawFullCapacity)
            if (resolvedHealth != null && resolvedHealth in 1..100) {
                setPreferenceText(pref, "$resolvedHealth%")
            }
        }
    }

    private fun resolveHealthPercent(rawDesign: Long?, rawFull: Long?): Int? {
        val miChargeSoh = runCatching {
            val miChargeClass = findClassIfExists("miui.util.IMiCharge") ?: return@runCatching null
            val instance = miChargeClass.getMethod("getInstance").invoke(null) ?: return@runCatching null
            val soh = instance.javaClass.getMethod("getBatterySoh").invoke(instance)
            when (soh) {
                is Int -> if (soh in 1..100) soh else null
                is Number -> if (soh.toInt() in 1..100) soh.toInt() else null
                is String -> soh.trim().toIntOrNull()?.takeIf { it in 1..100 }
                null -> null
                else -> null
            }
        }.getOrNull()
        if (miChargeSoh != null) {
            return miChargeSoh
        }

        val sysfsSoh = readFirstPositiveNumber(listOf(
            "$BATTERY_SYSFS_DIR/soh",
            "/sys/class/power_supply/mtk-gauge/soh",
            "$BATTERY_BMS_SYSFS_DIR/soh"
        ))
        if (sysfsSoh != null && sysfsSoh in 1..100) {
            return sysfsSoh
        }

        if (rawDesign != null && rawFull != null && rawDesign > 0 && rawFull > 0) {
            val ratio = kotlin.math.round((rawFull.toDouble() / rawDesign.toDouble()) * 100.0).toInt()
            return if (ratio > 100) 100 else if (ratio < 1) null else ratio
        }
        return null
    }
}
