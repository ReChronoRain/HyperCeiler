package com.sevtinge.hyperceiler.module.hook.updater

import com.sevtinge.hyperceiler.module.base.BaseHook

class AutoUpdateDialog : BaseHook() {
    override fun init() {
        //TODO 不写死程序
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
    }
}