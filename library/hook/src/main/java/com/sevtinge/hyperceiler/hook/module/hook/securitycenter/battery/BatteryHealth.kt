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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter.battery

import android.os.Message
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.utils.callMethod
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method


object BatteryHealth : BaseHook() {
    private val getSecurityBatteryHealth by lazy<Method> {
        DexKit.findMember("getSecurityBatteryHealth") {
            it.findMethod {
                matcher {
                    addUsingString("battery_health_soh", StringMatchType.Equals)
                }
            }.single()
        }
    }

    private val cc by lazy<Method> {
        DexKit.findMember("SecurityBatteryHealthMethod") {
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
        DexKit.findMember("ChargeFragmentMethod") {
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

    private lateinit var gff: Any
    private var health: Int? = null


    override fun init() {
        getSecurityBatteryHealth.createAfterHook { param ->
            health = param.args[0] as Int // 获取手机管家内部的健康度
        }

        findMethod.createAfterHook { param ->
            gff = param.thisObject
                .callMethod("findPreference", "reference_battery_health")!!
        }

        cc.createAfterHook {
            gff.callMethod("setText", "$health %")
        }

    }
}
