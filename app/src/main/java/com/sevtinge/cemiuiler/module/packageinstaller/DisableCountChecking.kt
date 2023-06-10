package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object DisableCountChecking : BaseHook() {
    override fun init() {
        val riskControlRulesClass = loadClass("com.miui.packageInstaller.model.RiskControlRules")
        try {
            riskControlRulesClass.methodFinder().filterByName("getCurrentLevel").first().createHook {
                returnConstant(0)
            }
        } catch (t: Throwable) {
            logE(t)
        }
    }
}
