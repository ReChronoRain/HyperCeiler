package com.sevtinge.cemiuiler.module.mishare

import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.hostDir
import com.sevtinge.cemiuiler.utils.DexKit.loadDexKit
import io.luckypray.dexkit.enums.MatchType

class NoAutoTurnOff : BaseHook() {
    override fun init() {
        // 暂时先这样发版，等后面再改
        hostDir = lpparam.appInfo.sourceDir
        loadDexKit()
        dexKitBridge.batchFindMethodsUsingStrings {
            addQuery("qwq", listOf("EnabledState", "mishare_enabled"))
            matchType = MatchType.FULL
        }.forEach { (_, classes) ->
            classes.map {
                it.getMethodInstance(lpparam.classLoader)
            }.createHooks {
                before {
                    it.result = null
                }
            }
        }

        dexKitBridge.batchFindClassesUsingStrings {
            addQuery("qwq", listOf("null context", "cta_agree"))
            matchType = MatchType.FULL
        }.forEach { (_, classes) ->
            classes.map {
                it.getClassInstance(classLoader).methodFinder()
                    .filterByReturnType(Boolean::class.java)
                    .filterByParamCount(2)
                    .filterByParamTypes(Context::class.java, String::class.java)
                    .toList().createHooks {
                        before { param ->
                            if (param.args[1].equals("security_agree")) {
                                param.result = false
                            }
                        }
                    }
            }
        }
        closeDexKit()
    }
}