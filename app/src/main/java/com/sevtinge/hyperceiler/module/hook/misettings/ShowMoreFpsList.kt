package com.sevtinge.hyperceiler.module.hook.misettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object ShowMoreFpsList : BaseHook() {
    override fun init() {
        loadClass("miui.util.FeatureParser").methodFinder().first {
            name == "getIntArray"
        }.createHook {
            before {
                if (it.args[0] == "fpsList") {
                    it.result = intArrayOf(144, 120, 90, 60, 30)
                }
            }
        }
    }
}
