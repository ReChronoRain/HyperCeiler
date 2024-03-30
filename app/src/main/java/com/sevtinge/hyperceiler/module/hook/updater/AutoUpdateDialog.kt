package com.sevtinge.hyperceiler.module.hook.updater

import com.github.kyuubiran.ezxhelper.*
import com.sevtinge.hyperceiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge.*

class AutoUpdateDialog : BaseHook() {
    override fun init() {
        // TODO 不写死程序
        try {
            findAndHookMethod(
                "com.android.updater.i1",
                "Z2",
                Boolean::class.java,
                Boolean::class.java,
                object : replaceHookedMethod() {
                    override fun replace(param: MethodHookParam?): Any {
                        return 0
                    }
                }
            )
        } catch (e: Exception) {
            log(e)
        }

        try {
            findAndHookMethod(
                "com.android.updater.i1",
                "q3",
                Long::class.java,
                Int::class.java,
                object : replaceHookedMethod() {
                    override fun replace(param: MethodHookParam?): Any {
                        return 0
                    }
                }
            )
        } catch (e: Exception) {
            log(e)
        }
    }
}