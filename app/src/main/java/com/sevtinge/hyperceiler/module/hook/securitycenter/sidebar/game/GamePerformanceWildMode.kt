package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

class GamePerformanceWildMode : BaseHook() {
    override fun init() {
        // 开放均衡/狂暴模式
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("support_wild_boost")
            }
        }.first().getMethodInstance(safeClassLoader).createHook {
            returnConstant(true)
        }
    }
}
