package com.sevtinge.hyperceiler.module.hook.home.recent

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object MemInfoShow : BaseHook() {
    override fun init() {
        loadClass("com.miui.home.recents.views.RecentsDecorations").methodFinder().first {
            name == "canTxtMemInfoShow"
        }.createHook {
            before { param ->
                param.result = true
            }
        }
    }
}
