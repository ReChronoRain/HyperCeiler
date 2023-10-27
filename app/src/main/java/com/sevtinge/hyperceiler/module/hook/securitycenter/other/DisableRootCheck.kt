package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object DisableRootCheck : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("key_check_item_root")
                returnType = "boolean"
            }
        }.firstOrNull()?.getMethodInstance(lpparam.classLoader)?.createHook {
            returnConstant(false)
        }

        /*try {
            val result: List<DexMethodDescriptor> =
                Objects.requireNonNull<List<DexMethodDescriptor>>(
                    SecurityCenterDexKit.mSecurityCenterResultMap["rootCheck"]
                )
            for (descriptor in result) {
                val checkIsRooted: Method = descriptor.getMethodInstance(lpparam.classLoader)
                if (checkIsRooted.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(
                        checkIsRooted,
                        XC_MethodReplacement.returnConstant(false)
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }*/
    }
}
