package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

class VBVideoMode : BaseHook() {
    override fun init() {
        // 开放影院/自定义模式
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("TheatreModeUtils")
                usingNumbers = listOf(32)
            }
        }.first().getMethodInstance(safeClassLoader).createHook {
            returnConstant(true)
        }
    }
}
