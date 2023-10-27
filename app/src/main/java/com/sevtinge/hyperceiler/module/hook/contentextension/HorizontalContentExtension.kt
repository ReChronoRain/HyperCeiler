package com.sevtinge.hyperceiler.module.hook.contentextension

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object HorizontalContentExtension : BaseHook() {
    override fun init() {
        loadClass("com.miui.contentextension.services.TextContentExtensionService").methodFinder()
            .first {
                name == "isScreenPortrait"
            }.createHook {
            after {
                it.result = true
            }
        }
    }
}
