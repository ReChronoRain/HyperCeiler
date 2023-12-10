package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object UnlockSuperWirelessCharge : BaseHook() {

    private val superWirelessCharge by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("persist.vendor.tx.speed.control")
                returnType = "boolean"
            }
        }.single().getMethodInstance(EzXHelper.classLoader)
    }

    private val superWirelessChargeTip by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("key_is_connected_super_wls_tx")
                returnType = "boolean"
            }
        }.single().getMethodInstance(EzXHelper.classLoader)
    }

    override fun init() {
        superWirelessCharge.createHook {
            returnConstant(true)
        }

        superWirelessChargeTip.createHook {
            returnConstant(true)
        }
    }
}
