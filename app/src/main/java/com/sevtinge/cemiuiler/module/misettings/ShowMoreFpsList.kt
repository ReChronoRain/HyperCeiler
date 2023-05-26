package com.sevtinge.cemiuiler.module.misettings

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

object ShowMoreFpsList : BaseHook() {
    override fun init() {
        findMethod("miui.util.FeatureParser") {
            name == "getIntArray"
        }.hookBefore {
            if (it.args[0] == "fpsList") {
                it.result = intArrayOf(144, 120, 90, 60, 30)
            }
        }
    }
}
