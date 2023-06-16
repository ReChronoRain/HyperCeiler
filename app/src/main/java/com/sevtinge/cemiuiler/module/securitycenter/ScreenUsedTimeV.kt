package com.sevtinge.cemiuiler.module.securitycenter

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.loadDexKit
import io.luckypray.dexkit.enums.MatchType

object ScreenUsedTimeV : BaseHook() {
    override fun init() {
        initDexKit(lpparam)
        loadDexKit()

        val classesName = dexKitBridge.batchFindClassesUsingStrings {
            addQuery("qwq1", listOf("not support screenPowerSplit", "PowerRankHelperHolder"))
            matchType = MatchType.FULL
        }.values
            .flatten()
            .map { it.getClassInstance(EzXHelper.classLoader).name }
            .firstOrNull()

        val methods1 = dexKitBridge.batchFindMethodsUsingStrings {
            addQuery("qwq2", listOf("ishtar", "nuwa", "fuxi"))
            matchType = MatchType.FULL
        }.values
            .flatten()
            .map { it.getMethodInstance(EzXHelper.classLoader) }
            .firstOrNull()

        dexKitBridge.findMethod {
            methodDeclareClass = classesName!!
            methodReturnType = "boolean"
            methodParamTypes = arrayOf()
        }.forEach { methods ->
            val methods2 = methods.getMethodInstance(EzXHelper.classLoader)
            methods2.createHook {
                returnConstant(
                    when (methods2) {
                        methods1 -> true
                        else -> false
                    }
                )
            }
        }

        closeDexKit()
    }
}
