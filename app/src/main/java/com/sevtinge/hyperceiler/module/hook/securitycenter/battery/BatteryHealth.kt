package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import android.os.Bundle
import android.os.Message
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit
import de.robv.android.xposed.XC_MethodHook
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

    private lateinit var gff: Any
    private var health: Int? = null


    override fun init() {
        getSecurityBatteryHealth.createHook {
            after { params ->
                health = params.args[0] as Int // 获取手机管家内部的健康度
            }
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

        findAndHookMethod(
            cc.single().name,
            "handleMessage",
            Message::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    gff.callMethod("setText", "$health %")
                }
            })
    }
}
