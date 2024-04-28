package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import org.luckypray.dexkit.query.enums.*


object BatteryHealth : BaseHook() {
    private val dexKitBridge = DexKit.getDexKitBridge()

    private val getSecurityBatteryHealth by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingString("battery_health_soh", StringMatchType.Equals)
            }
        }.single().getMethodInstance(EzXHelper.classLoader)
    }

    private val c = dexKitBridge.findClass {
        searchPackages("com.miui.powercenter.nightcharge")
        findFirst = true
        matcher {
            className = "com.miui.powercenter.nightcharge.SmartChargeFragment"
        }
    }

    private val onc = dexKitBridge.findMethod {
        findFirst = true
        searchInClass(c)
        matcher {
            name = "onCreatePreferences"
        }
    }.single().getMethodInstance(EzXHelper.classLoader)

    private val cc = dexKitBridge.findClass {
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

    val m = dexKitBridge.findMethod {
        searchInClass(cc)
        findFirst = true
        matcher {
            name = "handleMessage"
        }
    }.single().getMethodInstance(EzXHelper.classLoader)

    private lateinit var gff: Any


    override fun init() {
        onc.createHook {
            after {
                gff = it.thisObject
                    .callMethod("findPreference", "reference_battery_health")!!
            }
        }

        getSecurityBatteryHealth.createHook {
            after { params ->
                val health = params.args[0] as Int // 获取手机管家内部的健康度

                m.createHook {
                    after {
                        gff.callMethod("setText", "$health %")
                    }
                }
            }
        }
    }
}
