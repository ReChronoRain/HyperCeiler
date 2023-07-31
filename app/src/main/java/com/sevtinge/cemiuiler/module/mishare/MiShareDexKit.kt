package com.sevtinge.cemiuiler.module.mishare

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.safeDexKitBridge
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import io.luckypray.dexkit.enums.MatchType

class MiShareDexKit : BaseHook() {
    override fun init() {
        System.loadLibrary("dexkit")
        initDexKit(lpparam)
        try {
            mMiShareResultMethodsMap = safeDexKitBridge.batchFindMethodsUsingStrings {
                addQuery("qwq", setOf("EnabledState", "mishare_enabled"))
                matchType = MatchType.FULL
            }
            mMiShareResultClassMap = safeDexKitBridge.batchFindClassesUsingStrings {
                addQuery("qwq2", setOf("null context", "cta_agree"))
                matchType = MatchType.FULL
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        closeDexKit()
    }

    companion object {
        var mMiShareResultMethodsMap: Map<String, List<DexMethodDescriptor>>? = null
        var mMiShareResultClassMap: Map<String, List<DexClassDescriptor>>? = null
    }
}
