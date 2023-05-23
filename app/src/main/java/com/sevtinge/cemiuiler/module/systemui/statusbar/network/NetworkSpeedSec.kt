package com.sevtinge.cemiuiler.module.systemui.statusbar.network

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

//隐藏网速单位
object NetworkSpeedSec : BaseHook() {
    override fun init() {
        findMethod("com.android.systemui.statusbar.views.NetworkSpeedView") {
            name == "setNetworkSpeed" && parameterCount == 1
        }.hookBefore {
            if (it.args[0] != null) {
                val mText = (it.args[0] as String)
                    .replace("/", "")
                    .replace("s", "")
                    .replace("\'", "")
                    .replace("วิ", "")
                it.args[0] = mText
            }
        }
    }
}