package com.sevtinge.cemiuiler.module.hook.home

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.enums.MatchType

class HomeDexKit : BaseHook() {
    override fun init() {
        initDexKit(lpparam)
        try {
            mHomeResultClassMap = dexKitBridge.batchFindClassesUsingStrings {
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
