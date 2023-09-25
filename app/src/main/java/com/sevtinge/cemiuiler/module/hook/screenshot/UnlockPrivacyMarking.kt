package com.sevtinge.cemiuiler.module.hook.screenshot

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object UnlockPrivacyMarking : BaseHook() {
    override fun init() {
        val isClass by lazy {
            loadClass("com.miui.gallery.editor.photo.screen.mosaic.ScreenMosaicView")
        }

        isClass.methodFinder().first {
            name == "isSupportPrivacyMarking"
        }.createHook {
            try {
                returnConstant(true)
            } catch (e: Exception) {
                logI("UnSupport Privacy Marking")
            }
        }
    }
}
