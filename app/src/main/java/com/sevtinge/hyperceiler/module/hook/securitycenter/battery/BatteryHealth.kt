package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import android.os.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toClass
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
        DexKit.getDexKitBridge("getSecurityBatteryHealthClass") {
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
            }.first().getInstance(EzXHelper.safeClassLoader)
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

        findAndHookMethod(
            cc.toClass(),
            "handleMessage",
            Message::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    gff.callMethod("setText", "$health %")
                }
            })
    }
}
