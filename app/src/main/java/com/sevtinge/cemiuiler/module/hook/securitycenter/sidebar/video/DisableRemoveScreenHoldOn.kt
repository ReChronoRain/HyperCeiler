package com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.video

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object DisableRemoveScreenHoldOn : BaseHook() {
    /*override fun init() {
        try {
            val result: List<DexMethodDescriptor> =
                Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("RemoveScreenHoldOn"))
            for (descriptor in result) {
                val removeScreenHoldOn: Method = descriptor.getMethodInstance(lpparam.classLoader)
                log("removeScreenHoldOn method is $removeScreenHoldOn")
                if (removeScreenHoldOn.returnType == Boolean::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(
                        removeScreenHoldOn,
                        XC_MethodReplacement.returnConstant(false)
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }*/

    private val screen by lazy {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("remove_screen_off_hold_on")
                returnType = "boolean"
            }
        }.firstOrNull()?.getMethodInstance(EzXHelper.classLoader)
    }

    override fun init() {
        screen?.createHook {
            before {
                it.result = false
            }
        }
    }
}
