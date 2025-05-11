package com.sevtinge.hyperceiler.hook.module.hook.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook

object UnlockXiaomiHyperAIEntranceKt : BaseHook() {

    @Override
    override fun init() {
        loadClass("com.android.settings.InternalDeviceUtils").methodFinder()
            .filterByName("isAiSupported")
            .first().createHook {
                returnConstant(true)
            }
    }
}