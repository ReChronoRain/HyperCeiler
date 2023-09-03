package com.sevtinge.cemiuiler.module.hook.miinput

import android.content.Context
import com.sevtinge.cemiuiler.module.base.BaseHook

object UnlockKnuckleFunction : BaseHook() {
    override fun init() {
        findAndHookMethod(
            "com.android.settings.MiuiShortcut\$System",
            "hasKnockFeature",
            Context::class.java,
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    param.result = true
                }
            })
    }
}
