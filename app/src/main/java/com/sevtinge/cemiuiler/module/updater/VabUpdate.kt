package com.sevtinge.cemiuiler.module.updater

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

class VabUpdate : BaseHook() {
    override fun init() {
        findMethod("miui.util.FeatureParser") {
            name == "hasFeature" && parameterCount == 2
        }.hookBefore {
            if (it.args[0] == "support_ota_validate") {
                it.result = false
            }
        }
    }

}