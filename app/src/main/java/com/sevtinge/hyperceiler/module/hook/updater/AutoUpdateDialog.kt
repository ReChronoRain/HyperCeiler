package com.sevtinge.hyperceiler.module.hook.updater

import com.sevtinge.hyperceiler.module.base.BaseHook

class AutoUpdateDialog : BaseHook() {
    override fun init() {
        println("*AUD")
        findAndHookMethod(
            "com.android.updater.i1",
            "Z2",
            Boolean::class.java,
            Boolean::class.java,
            object : replaceHookedMethod() {
                override fun replace(param: MethodHookParam?): Any {
                    println("asd")
                    return 0
                }
            }
        )
    }
}