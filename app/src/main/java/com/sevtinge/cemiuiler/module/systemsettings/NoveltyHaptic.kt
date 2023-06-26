package com.sevtinge.cemiuiler.module.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object NoveltyHaptic : BaseHook() {
    override fun init() {
        if (mPrefsMap.getBoolean("system_settings_international_build")) return // 开启国际版设置界面将禁用此功能
        loadClass("com.android.settings.utils.SettingsFeatures").methodFinder().first {
            name == "isNoveltyHaptic"
        }.createHook {
            returnConstant(true)
        }
    }
}
