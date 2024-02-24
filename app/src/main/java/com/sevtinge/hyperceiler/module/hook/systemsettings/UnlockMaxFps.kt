package com.sevtinge.hyperceiler.module.hook.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook

object UnlockMaxFps : BaseHook() {
    override fun init() {
        // by TG@Crystal
        loadClass("com.android.settings.development.ForcePeakRefreshRatePreferenceController").methodFinder()
            .filterByName("isAvailable")
            .single().createHook {
                after {
                    it.result = true
                }
            }
    }
}
