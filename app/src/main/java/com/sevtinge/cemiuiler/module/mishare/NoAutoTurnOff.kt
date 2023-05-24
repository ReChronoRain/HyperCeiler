package com.sevtinge.cemiuiler.module.mishare

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers

class NoAutoTurnOff : BaseHook() {

    override fun init() {
        when (val version = Helpers.getPackageVersionCode(lpparam)) {
            21500 -> {
                findMethod("com.miui.mishare.connectivity.MiShareService\$d\$g") {
                    name == "b"
                }.hookBefore {
                    it.result = null
                }
            }

            21600 -> {
                findMethod("com.miui.mishare.connectivity.MiShareService\$j\$g") {
                    name == "a"
                }.hookBefore {
                    it.result = null
                }
            }

            else ->  log("Your MiShare version is $version, NoAutoTurnOff doesn't work")
        }
    }
}