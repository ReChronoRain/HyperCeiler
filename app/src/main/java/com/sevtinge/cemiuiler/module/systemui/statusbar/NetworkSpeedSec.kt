package com.sevtinge.cemiuiler.module.systemui.statusbar

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

//隐藏网速单位，仅供 Android12 及以上使用
object NetworkSpeedSec : BaseHook() {
    override fun init() {
        findMethod("com.android.systemui.statusbar.views.NetworkSpeedView") {
            name == "setNetworkSpeed" && parameterCount == 1
        }.hookBefore {
           if (mPrefsMap.getBoolean("hide_status_bar_network_speed_second")) {
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
}