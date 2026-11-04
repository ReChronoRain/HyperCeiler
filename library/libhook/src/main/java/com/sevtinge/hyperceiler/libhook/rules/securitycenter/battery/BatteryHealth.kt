/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery

import android.os.Message
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File
import java.lang.reflect.Method

/**
 * Battery health hook for Security Center.
 * Hooks battery health display and provides fallback resolution.
 */
object BatteryHealth : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        getSecurityBatteryHealth
        cc
        findMethod
        return true
    }

    private const val METHOD_FIND_PREFERENCE = "findPreference"
    private const val METHOD_SET_TEXT = "setText"
    private const val KEY_BATTERY_HEALTH = "reference_battery_health"

    private val getSecurityBatteryHealth by lazy<Method?> {
        optionalMember("getSecurityBatteryHealth") {
            it.findMethod {
                matcher {
                    addUsingString("battery_health_soh", StringMatchType.Equals)
                }
            }.firstOrNull()
        }
    }

    private val cc by lazy<Method> {
        requiredMember("SecurityBatteryHealthMethod") {
            it.findMethod {
                searchPackages("com.miui.powercenter.nightcharge")
                findFirst = true
                matcher {
                    name = "handleMessage"
                    paramTypes = listOf(Message::class.java.name)
                }
            }.single()
        }
    }

    private val findMethod by lazy<Method> {
        requiredMember("ChargeFragmentMethod") {
            it.findMethod {
                searchPackages("com.miui.powercenter.nightcharge")
                findFirst = true
                matcher {
                    name = "onCreatePreferences"
                    paramCount = 2
                }
            }.single()
        }
    }

    private var gff: Any? = null
    private var health: Int? = null

    override fun init() {
        getSecurityBatteryHealth?.createAfterHook { param ->
            val arg = param.args.firstOrNull()
            val parsed = when (arg) {
                is Int -> if (arg > 0) arg else null
                is Number -> if (arg.toInt() > 0) arg.toInt() else null
                is String -> arg.trim().toIntOrNull()?.takeIf { it > 0 }
                else -> null
            }
            if (parsed != null) {
                health = parsed
            }
        }

        findMethod.createAfterHook { param ->
            gff = runCatching {
                param.thisObject.callMethod(METHOD_FIND_PREFERENCE, KEY_BATTERY_HEALTH)
            }.getOrNull()
        }

        cc.createAfterHook {
            val pref = gff
            if (pref != null) {
                val resolvedHealth = health ?: resolveHealthFallback()
                if (resolvedHealth != null && resolvedHealth in 1..100) {
                    runCatching {
                        pref.callMethod(METHOD_SET_TEXT, "$resolvedHealth%")
                    }
                }
            }
        }
    }

    private fun resolveHealthFallback(): Int? {
        val miChargeSoh = readMiChargeSoh()
        if (miChargeSoh != null) {
            return miChargeSoh
        }

        for (path in listOf(
            "/sys/class/power_supply/battery/soh",
            "/sys/class/power_supply/mtk-gauge/soh",
            "/sys/class/power_supply/bms/soh"
        )) {
            val value = readSysfsInt(path)
            if (value != null && value in 1..100) {
                return value
            }
        }

        val rawDesign = readFirstPositiveSysfsLong(listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/bms/charge_full_design"
        ))
        val rawFull = readFirstPositiveSysfsLong(listOf(
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/bms/charge_full"
        ))
        val design = rawDesign ?: 0L
        val full = rawFull ?: 0L
        if (design > 0L && full > 0L) {
            val ratio = kotlin.math.round((full.toDouble() / design.toDouble()) * 100.0).toInt()
            return if (ratio > 100) 100 else if (ratio < 1) null else ratio
        }

        return null
    }

    private fun readMiChargeSoh(): Int? {
        return try {
            val miChargeClass = findClassIfExists("miui.util.IMiCharge")
            val instance = miChargeClass?.getMethod("getInstance")?.invoke(null)
            if (instance != null) {
                val soh = instance.javaClass.getMethod("getBatterySoh").invoke(instance)
                when (soh) {
                    is Int -> if (soh in 1..100) soh else null
                    is Number -> if (soh.toInt() in 1..100) soh.toInt() else null
                    is String -> soh.trim().toIntOrNull()?.takeIf { it in 1..100 }
                    null -> null
                    else -> null
                }
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun readSysfsInt(path: String): Int? {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.bufferedReader().use { it.readLine()?.trim()?.toIntOrNull() }
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun readFirstPositiveSysfsLong(paths: List<String>): Long? {
        for (path in paths) {
            val value = try {
                val file = File(path)
                if (file.exists()) {
                    file.bufferedReader().use { it.readLine()?.trim()?.toLongOrNull() }
                } else null
            } catch (_: Throwable) {
                null
            }
            if (value != null && value > 0L) {
                return value
            }
        }
        return null
    }
}
