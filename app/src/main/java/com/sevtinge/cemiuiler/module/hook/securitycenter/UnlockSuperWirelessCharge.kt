package com.sevtinge.cemiuiler.module.hook.securitycenter

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object UnlockSuperWirelessCharge : BaseHook() {

    private val superWirelessCharge by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("persist.vendor.tx.speed.control")
                returnType = "boolean"
            }
        }
    }

    private val superWirelessChargeTip by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("key_is_connected_super_wls_tx")
                returnType = "boolean"
            }
        }
    }

    override fun init() {
        superWirelessCharge.forEach {
            it.getMethodInstance(lpparam.classLoader).createHook {
                returnConstant(true)
            }
        }

        superWirelessChargeTip.forEach {
            it.getMethodInstance(lpparam.classLoader).createHook {
                returnConstant(true)
            }
        }

        /*try {
            val result: List<DexMethodDescriptor> =
                java.util.Objects.requireNonNull<List<DexMethodDescriptor>>(
                    SecurityCenterDexKit.mSecurityCenterResultMap["SuperWirelessCharge"]
                )
            for (descriptor in result) {
                val SuperWirelessCharge: java.lang.reflect.Method =
                    descriptor.getMethodInstance(lpparam.classLoader)
                log("SuperWirelessCharge method is $SuperWirelessCharge")
                if (SuperWirelessCharge.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(
                        SuperWirelessCharge,
                        XC_MethodReplacement.returnConstant(true)
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            val result: List<DexMethodDescriptor> =
                java.util.Objects.requireNonNull<List<DexMethodDescriptor>>(
                    SecurityCenterDexKit.mSecurityCenterResultMap["SuperWirelessChargeTip"]
                )
            for (descriptor in result) {
                val SuperWirelessChargeTip: java.lang.reflect.Method =
                    descriptor.getMethodInstance(lpparam.classLoader)
                log("SuperWirelessChargeTip method is $SuperWirelessChargeTip")
                if (SuperWirelessChargeTip.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(
                        SuperWirelessChargeTip,
                        XC_MethodReplacement.returnConstant(true)
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }*/
    }
}
