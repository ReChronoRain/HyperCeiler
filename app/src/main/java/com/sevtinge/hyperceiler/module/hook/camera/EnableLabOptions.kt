package com.sevtinge.hyperceiler.module.hook.camera

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.hookBeforeMethod


object EnableLabOptions : BaseHook() {
    override fun init() {
        try {
            "com.xiaomi.camera.util.SystemProperties".hookBeforeMethod(
                "getBoolean", String::class.java, Boolean::class.java
            ) {
                if (it.args[0] == "camera.lab.options") it.result = true
            }
        } catch (e: Exception) {
            logE(TAG, this.lpparam.packageName, e)
        }
    }
}
