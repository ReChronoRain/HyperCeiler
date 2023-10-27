package com.sevtinge.hyperceiler.module.hook.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.LazyClass.SettingsFeaturesClass

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
