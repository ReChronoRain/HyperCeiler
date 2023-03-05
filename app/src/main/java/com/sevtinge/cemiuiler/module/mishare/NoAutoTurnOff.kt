package com.sevtinge.cemiuiler.module.mishare

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

class NoAutoTurnOff : BaseHook(){

    override fun init() {
        try {
            findMethod("com.miui.mishare.connectivity.MiShareService\$d\$g") {
                name == "b"
            }.hookBefore {
                it.result = null
            }
            XposedBridge.log("Voyager-Test: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService success!")
        } catch (e: Throwable) {
            XposedBridge.log("Voyager-Test: NoAutoTurnOff com.miui.mishare.connectivity.MiShareService failed!")
            XposedBridge.log(e)
        }
    }
}