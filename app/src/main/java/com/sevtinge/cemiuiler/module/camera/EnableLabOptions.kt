package com.sevtinge.cemiuiler.module.camera

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.hookBeforeMethod

object EnableLabOptions : BaseHook() {
    override fun init() {
        try {
            "com.xiaomi.camera.util.SystemProperties".hookBeforeMethod(
                "getBoolean", String::class.java, Boolean::class.java
            ) {
                if (it.args[0] == "camera.lab.options") it.result = true
            }
        } catch (e: Throwable) {
            logE(e)
        }
    }
}
