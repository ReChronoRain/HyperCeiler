package com.sevtinge.cemiuiler.module.home.recent

import com.sevtinge.cemiuiler.module.base.BaseHook

class MemInfoShow : BaseHook() {
    override fun init() {
        hookAllMethods(
            "com.miui.home.recents.views.RecentsDecorations",
            "canTxtMemInfoShow",
            object : MethodHook() {
                override fun before(param: MethodHookParam) {
                    param.result = true
                }
            })
    }
}
