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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import android.os.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toElementList
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import com.sevtinge.hyperceiler.utils.*
import de.robv.android.xposed.*
import org.luckypray.dexkit.query.enums.*


object BatteryHealth : BaseHook() {
    private val getSecurityBatteryHealth by lazy {
        DexKit.getDexKitBridge("getSecurityBatteryHealth") {
            it.findMethod {
                matcher {
                    addUsingString("battery_health_soh", StringMatchType.Equals)
                }
            }.single().getMethodInstance(EzXHelper.classLoader)
        }.toMethod()
    }

    private val cc by lazy {
        DexKit.useDexkitIfNoCache(arrayOf("SecurityBatteryHealthClass")) {
            it.findClass {
                searchPackages("com.miui.powercenter.nightcharge")
                findFirst = true
                matcher {
                    methods {
                        add {
                            name = "handleMessage"
                        }
                    }
                }
            }
        }
    }

    private lateinit var gff: Any
    private var health: Int? = null


    override fun init() {
        getSecurityBatteryHealth.createAfterHook { param ->
            health = param.args[0] as Int // 获取手机管家内部的健康度
        }

        findAndHookMethod(
            "com.miui.powercenter.nightcharge.SmartChargeFragment",
            "onCreatePreferences",
            Bundle::class.java, String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    gff = param.thisObject
                        .callMethod("findPreference", "reference_battery_health")!!
                }
            }
        )

        val nameClass = DexKit.getDexKitBridgeList("SecurityBatteryHealthClass") { _ ->
            cc?.toElementList()
        }.toClassList().first().name
        findAndHookMethod(
            nameClass,
            "handleMessage",
            Message::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    gff.callMethod("setText", "$health %")
                }
            })
    }
}
