package com.sevtinge.cemiuiler.module.hook.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.LazyClass.SettingsFeaturesClass

class EnableFoldArea : BaseHook() {
    override fun init() {
        setStaticObject(
            SettingsFeaturesClass,
            "IS_SUPPORT_FOLD_SCREEN_SETTINGS",
            true
        )

        SettingsFeaturesClass.methodFinder().first(){
            name == "isSupportFoldScreenSettings"
        }.createHook {
            before{
                it.result = true
            }
        }
    }
}
