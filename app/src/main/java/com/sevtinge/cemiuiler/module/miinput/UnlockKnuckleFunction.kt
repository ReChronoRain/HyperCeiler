package com.sevtinge.cemiuiler.module.miinput

import com.sevtinge.cemiuiler.module.base.BaseHook


object UnlockKnuckleFunction : BaseHook() {
    override fun init() {
        findAndHookMethod("com.android.settings.MiuiShortcut\$System",
            "hasKnockFeature",  //Context:class
            object : MethodHook() {
                override fun before(param: MethodHookParam?) {
                    param?.result = true
                }
            })

    }

}