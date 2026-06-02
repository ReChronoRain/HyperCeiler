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
import java.lang.ref.WeakReference
import java.lang.reflect.Method


object BatteryHealth : BaseHook() {
    private const val PREF_KEY_BATTERY_HEALTH = "reference_battery_health"

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        getSecurityBatteryHealth
        cc
        findMethod
        return true
    }

    private val getSecurityBatteryHealth by lazy<Method> {
        requiredMember("getSecurityBatteryHealth") {
            it.findMethod {
                matcher {
                    addUsingString("battery_health_soh", StringMatchType.Equals)
                }
            }.single()
        }
    }

    /** ChargeProtectFragment$d#handleMessage 的查找。 */
    private val cc by lazy<Method> {
        requiredMember("SecurityBatteryHealthMethod") {
            it.findMethod {
                searchPackages("com.miui.powercenter.nightcharge")
                findFirst = true
                matcher {
                    name = "handleMessage"
                    paramTypes(Message::class.java.name)
                }
            }.single()
        }
    }

    /** ChargeProtectFragment#onCreatePreferences。 */
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

    /** ChargeProtectFragment 实例的弱引用，避免随 Activity 销毁泄漏 fragment。 */
    private var fragmentRef: WeakReference<Any>? = null

    /** 最近一次读取到的电池健康度；尚未捕获时不刷新 UI，避免显示 "null %"。 */
    private var health: Int? = null


    override fun init() {
        getSecurityBatteryHealth.createAfterHook { param ->
            health = param.args[0] as? Int
        }

        findMethod.createAfterHook { param ->
            fragmentRef = WeakReference(param.thisObject)
        }

        cc.createAfterHook {
            val fragment = fragmentRef?.get() ?: return@createAfterHook
            val healthValue = health ?: return@createAfterHook
            val pref = runCatching {
                fragment.callMethod("findPreference", PREF_KEY_BATTERY_HEALTH)
            }.getOrNull() ?: return@createAfterHook
            pref.callMethod("setText", "$healthValue %")
        }
    }
}

