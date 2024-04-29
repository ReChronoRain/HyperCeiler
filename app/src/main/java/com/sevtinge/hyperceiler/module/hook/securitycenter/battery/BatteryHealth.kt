package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import android.os.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.utils.*
import org.luckypray.dexkit.query.enums.*


object BatteryHealth : BaseHook() {
    private val getSecurityBatteryHealth by lazy {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                addUsingString("battery_health_soh", StringMatchType.Equals)
            }
        }.single().getMethodInstance(EzXHelper.classLoader)
    }

    override fun init() {
        getSecurityBatteryHealth.createHook {
            after { params ->
                val health = params.args[0] as Int // 获取手机管家内部的健康度

                findClassIfExists("com.miui.powercenter.nightcharge.SmartChargeFragment\$c").let { c ->
                    c.hookAfterMethod("handleMessage", Message::class.java) {
                        // TODO hardcode。想办法改进
                        it.thisObject.getObjectField("a")!!.callMethod("get")!!.let { cc ->
                            cc.getObjectField("c")!!.callMethod("setText", "$health %")
                        }
                    }
                }
            }
        }
    }
}
