package com.sevtinge.hyperceiler.module.hook.packageinstaller

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

@SuppressLint("StaticFieldLeak")
object InstallRiskDisable : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("secure_verify_enable")
            }
        }.map { it.getMethodInstance(lpparam.classLoader) }.toList().createHooks {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("installerOpenSafetyModel")
            }
        }.map { it.getMethodInstance(lpparam.classLoader) }.toList().createHooks {
            returnConstant(false)
        }

        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("android.provider.MiuiSettings\$Ad")
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            returnConstant(false)
        }
    }
}
