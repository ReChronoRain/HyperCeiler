package com.sevtinge.cemiuiler.module.home.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class OverlapMode : BaseHook() {
    override fun init() {
        // Fold2 样式负一屏
        loadClass("com.miui.home.launcher.overlay.assistant.AssistantDeviceAdapter").methodFinder()
            .first {
                name == "inOverlapMode"
            }.createHook {
                returnConstant(true)
            }
    }
}
