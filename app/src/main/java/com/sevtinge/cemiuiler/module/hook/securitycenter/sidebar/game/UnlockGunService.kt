package com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.hook.securitycenter.SecurityCenterDexKit.mSecurityCenterResultClassMap

object UnlockGunService : BaseHook() {
    override fun init() {
        val gbGameCollimator = mSecurityCenterResultClassMap["GbGameCollimator"]!!
        assert(gbGameCollimator.size == 1)
        val gbGameCollimatorDescriptor = gbGameCollimator.first()
        val gbGameCollimatorClass: Class<*> = gbGameCollimatorDescriptor.getClassInstance(lpparam.classLoader)
        gbGameCollimatorClass.methodFinder().first {
            returnType == Boolean::class.java && parameterCount == 1
        }.createHook {
            logI("GunService class is $gbGameCollimatorClass")
            returnConstant(true)
        }
    }
}
