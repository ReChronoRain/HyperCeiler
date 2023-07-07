package com.sevtinge.cemiuiler.module.packageinstaller

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object DisableCountChecking : BaseHook() {
    override fun init() {
        loadClass("com.miui.packageInstaller.model.RiskControlRules").methodFinder().first {
            name == "getCurrentLevel"
        }.createHook {
            returnConstant(0)
        }
    }
}
