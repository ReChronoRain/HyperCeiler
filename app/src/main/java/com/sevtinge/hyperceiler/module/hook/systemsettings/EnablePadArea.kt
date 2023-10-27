package com.sevtinge.hyperceiler.module.hook.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.LazyClass.SettingsFeaturesClass

class EnablePadArea : BaseHook() {
    override fun init() {
        setStaticObject(
            SettingsFeaturesClass,
            "IS_SUPPORT_TABLET_SCREEN_SETTINGS",
            true
        )
    }
}
