package com.sevtinge.hyperceiler.module.hook.packageinstaller

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

object InstallRiskDisable : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingString("secure_verify_enable", StringMatchType.Equals)
                returnType = "boolean"
            }
            matcher {
                addUsingString("installerOpenSafetyModel", StringMatchType.Equals)
                returnType = "boolean"
            }
            matcher {
                addUsingString("android.provider.MiuiSettings\$Ad", StringMatchType.Equals)
                returnType = "boolean"
            }
        }.map { it.getMethodInstance(lpparam.classLoader) }.toList().createHooks {
            returnConstant(false)
        }
    }
}
