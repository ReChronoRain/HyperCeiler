package com.sevtinge.cemiuiler.module.mtb

import com.github.kyuubiran.ezxhelper.utils.field
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook

object BypassAuthentication : BaseHook() {
    override fun init() {
        try {
            findMethod("com.xiaomi.mtb.MtbApp") {
                name == "setMiServerPermissionClass"
            }.hookBefore {
                it.args[0] = 0
            }
        } catch (_: Throwable) {
        }

        try {
            findMethod("com.xiaomi.mtb.activity.ModemTestBoxMainActivity") {
                name == "updateClass"
            }.hookBefore {
                it.thisObject.field("mClassNet", true).set(it.thisObject, 0)
            }
        } catch (_: Throwable) {
        }

        try {
            findMethod("com.xiaomi.mtb.activity.ModemTestBoxMainActivity") {
                name == "initClassProduct"
            }.hookAfter {
                it.thisObject.field("mClassProduct", true).set(it.thisObject, 0)
            }
        } catch (_: Throwable) {
        }
    }
}
