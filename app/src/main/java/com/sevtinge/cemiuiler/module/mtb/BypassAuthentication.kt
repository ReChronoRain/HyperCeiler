package com.sevtinge.cemiuiler.module.mtb

import com.github.kyuubiran.ezxhelper.utils.*
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
                it.args[0] = 0
                it.thisObject.putObject("mClassNet", 0)
            }
        } catch (_: Throwable) {
        }

        try {
            findMethod("com.xiaomi.mtb.activity.ModemTestBoxMainActivity") {
                name == "initClassProduct"
            }.hookAfter {
                it.thisObject.putObject("mClassProduct", 0)
            }
        } catch (_: Throwable) {
        }
    }

}
