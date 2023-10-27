package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object FuckRiskPkg : BaseHook() {
    private val pkg by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals(
                    "riskPkgList", "key_virus_pkg_list", "show_virus_notification"
                )
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    override fun init() {
        pkg.createHooks {
            before { param ->
                param.result = null
            }
        }
    }
}
