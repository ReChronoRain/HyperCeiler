package com.sevtinge.cemiuiler.module.home

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.safeDexKitBridge
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.enums.MatchType

class HomeDexKit : BaseHook() {
    override fun init() {
        initDexKit(lpparam)
        try {
            mHomeResultClassMap = safeDexKitBridge.batchFindClassesUsingStrings {
                addQuery("HideAllApp", setOf("appInfo.packageName", "activityInfo"))
                matchType = MatchType.FULL
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        closeDexKit()
    }

    companion object {
        var mHomeResultClassMap: Map<String, List<DexClassDescriptor>>? = null
    }
}
