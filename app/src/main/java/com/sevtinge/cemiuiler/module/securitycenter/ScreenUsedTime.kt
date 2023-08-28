package com.sevtinge.cemiuiler.module.securitycenter

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Method
import java.util.Objects

object ScreenUsedTime : BaseHook() {
    private var powerRankHelperHolder: Class<*>? = null
    private var powerRankHelperHolderSdkHelper: Method? = null
    override fun init() {
        initDexKit(lpparam)
        try {
            val result = Objects.requireNonNull(
                SecurityCenterDexKit.mSecurityCenterResultClassMap["PowerRankHelperHolder"]
            )
            for (descriptor in result) {
                powerRankHelperHolder = descriptor.getClassInstance(lpparam.classLoader)
                log("powerRankHelperHolder class is $powerRankHelperHolder")
            }
        } catch (e: Throwable) {
            logE("PowerRankHelperHolder", e)
        }

        try {
            val result = Objects.requireNonNull(
                SecurityCenterDexKit.mSecurityCenterResultMap["PowerRankHelperHolderSdkHelper"]
            )
            for (descriptor in result) {
                powerRankHelperHolderSdkHelper = descriptor.getMethodInstance(lpparam.classLoader)
                log("powerRankHelperHolderSdkHelper method is $powerRankHelperHolderSdkHelper")
            }
        } catch (e: Throwable) {
            logE("PowerRankHelperHolderSdkHelper", e)
        }

        dexKitBridge.findMethod {
            methodDeclareClass = powerRankHelperHolder!!.name
            methodReturnType = "boolean"
            methodParamTypes = arrayOf()
        }.forEach { method ->
            val methods = method.getMethodInstance(EzXHelper.classLoader)
            log("powerRankHelperHolderMethod method is $methods")
            methods.createHook {
                returnConstant(
                    when (methods) {
                        powerRankHelperHolderSdkHelper -> true
                        else -> false
                    }
                )
            }
        }
        closeDexKit()
    }
}
