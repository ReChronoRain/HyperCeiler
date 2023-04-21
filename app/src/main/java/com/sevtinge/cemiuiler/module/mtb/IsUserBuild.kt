package com.sevtinge.cemiuiler.module.mtb

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.sevtinge.cemiuiler.module.base.BaseHook

object IsUserBuild : BaseHook() {
    override fun init() {
        try {
            findMethod("com.xiaomi.mtb.MtbUtils") {
                name == "IsUserBuild"
            }.hookReturnConstant(false)
        } catch (_: Throwable) {
        }
    }
}