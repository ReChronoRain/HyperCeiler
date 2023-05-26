package com.sevtinge.cemiuiler.module.mishare

import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.hostDir
import com.sevtinge.cemiuiler.utils.DexKit.loadDexKit
import io.luckypray.dexkit.enums.MatchType

class NoAutoTurnOff : BaseHook() {

    override fun init() {
        hostDir = lpparam.appInfo.sourceDir
        loadDexKit()
        dexKitBridge.batchFindMethodsUsingStrings {
            addQuery("qwq", listOf("EnabledState", "mishare_enabled"))
            matchType = MatchType.FULL
        }.forEach { (_, classes) ->
            classes.map {
                it.getMethodInstance(lpparam.classLoader)
            }.hookBefore {
                it.result = null
            }
        }

        dexKitBridge.batchFindClassesUsingStrings {
            addQuery("qwq", listOf("null context", "cta_agree"))
            matchType = MatchType.FULL
        }.forEach { (_, classes) ->
            classes.map {
                val qaq = it.getClassInstance(lpparam.classLoader)
                findAllMethods(qaq) {
                    returnType == Boolean::class.java && paramCount == 2 && parameterTypes[0] == Context::class.java && parameterTypes[1] == String::class.java
                }.hookBefore { param ->
                    if (param.args[1].equals("security_agree")) {
                        param.result = false
                    }
                }
            }
        }
        closeDexKit()
    }
}