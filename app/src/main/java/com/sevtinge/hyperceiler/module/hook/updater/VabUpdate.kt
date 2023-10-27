package com.sevtinge.hyperceiler.module.hook.updater

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

class VabUpdate : BaseHook() {
    override fun init() {
        loadClass("miui.util.FeatureParser").methodFinder().first {
            name == "hasFeature" && parameterCount == 2
        }.createHook {
            before {
                if (it.args[0] == "support_ota_validate") {
                    it.result = false
                }
            }
        }
    }

}
