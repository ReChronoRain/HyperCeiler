package com.sevtinge.hyperceiler.module.hook.securitycenter

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object IsSbnBelongToActiveBubbleApp : BaseHook() {
    override fun init() {
        runCatching {
            loadClass("com.miui.bubbles.settings.BubblesSettings").methodFinder().first {
                name == "isSbnBelongToActiveBubbleApp"
            }.createHook {
                returnConstant(true)
            }
        }
    }
}
