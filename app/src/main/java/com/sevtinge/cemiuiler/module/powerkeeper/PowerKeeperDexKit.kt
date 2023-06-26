package com.sevtinge.cemiuiler.module.powerkeeper

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.loadDexKit
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import io.luckypray.dexkit.enums.MatchType

class PowerKeeperDexKit : BaseHook() {
    override fun init() {
        System.loadLibrary("dexkit")
        initDexKit(lpparam)
        loadDexKit()
        try {
            mPowerKeeperResultMethodsMap = dexKitBridge.batchFindMethodsUsingStrings {
                addQuery("fucSwitch", listOf("custom_mode_switch", "fucSwitch"))
                matchType = MatchType.FULL
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        closeDexKit()
    }

    companion object {
        var mPowerKeeperResultMethodsMap: Map<String, List<DexMethodDescriptor>>? = null
    }
}
