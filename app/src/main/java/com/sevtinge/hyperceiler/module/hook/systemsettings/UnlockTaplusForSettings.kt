package com.sevtinge.hyperceiler.module.hook.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object UnlockTaplusForSettings : BaseHook() {
    override fun init() {
        loadClass("com.android.settings.utils.SettingsFeatures").methodFinder().first {
            name == "isNeedRemoveContentExtension"
        }.createHook {
            returnConstant(false)
        }

    }
}
