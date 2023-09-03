package com.sevtinge.cemiuiler.module.hook.systemui.statusbar.network

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

// 隐藏网速单位
object NetworkSpeedSec : BaseHook() {
    override fun init() {
        loadClass("com.android.systemui.statusbar.views.NetworkSpeedView").methodFinder().first {
            name == "setNetworkSpeed" && parameterCount == 1
        }.createHook {
            before {
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
