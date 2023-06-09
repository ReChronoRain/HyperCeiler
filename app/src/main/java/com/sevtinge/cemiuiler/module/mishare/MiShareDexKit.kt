package com.sevtinge.cemiuiler.module.mishare

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.hostDir
import com.sevtinge.cemiuiler.utils.DexKit.loadDexKit
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import io.luckypray.dexkit.enums.MatchType

class MiShareDexKit : BaseHook() {
    override fun init() {
        System.loadLibrary("dexkit")
        hostDir = lpparam.appInfo.sourceDir
        loadDexKit()
        try {
            mMiShareResultMethodsMap = dexKitBridge.batchFindMethodsUsingStrings {
                addQuery("qwq", listOf("EnabledState", "mishare_enabled"))
                matchType = MatchType.FULL
            }
            mMiShareResultClassMap = dexKitBridge.batchFindClassesUsingStrings {
                addQuery("qwq2", listOf("null context", "cta_agree"))
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
