package com.sevtinge.cemiuiler.module.mediaeditor

import com.github.kyuubiran.ezxhelper.utils.field
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import io.luckypray.dexkit.enums.MatchType

object FilterManagerAll : BaseHook() {
    override fun init() {
        DexKit.hostDir = lpparam.appInfo.sourceDir
        DexKit.loadDexKit()
        try {
            dexKitBridge.findMethodUsingString {
                usingString = "wayne"
                methodReturnType = "Ljava/util/List;"
                matchType = MatchType.FULL
            }.single().getMethodInstance(lpparam.classLoader).hookBefore {
                loadClass("android.os.Build").field("DEVICE", true, String::class.java)
                    .set(null, "wayne")
            }
        } catch (_: Throwable) {
            dexKitBridge.findMethodUsingString {
                usingString = "wayne"
                methodParamTypes = arrayOf("android.os.Bundle")
                matchType = MatchType.FULL
            }.single().getMethodInstance(lpparam.classLoader).hookBefore {
                loadClass("android.os.Build").field("DEVICE", true, String::class.java)
                    .set(null, "wayne")
            }
        }
    }
}