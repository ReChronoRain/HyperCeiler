package com.sevtinge.hyperceiler.module.hook.powerkeeper

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object CustomRefreshRate : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("custom_mode_switch", "fucSwitch")
            }
        }.single().getMethodInstance(lpparam.classLoader).createHook {
            before {
                ObjectUtils.setObject(it.thisObject, "mIsCustomFpsSwitch", "true")
            }
        }
    }
}
