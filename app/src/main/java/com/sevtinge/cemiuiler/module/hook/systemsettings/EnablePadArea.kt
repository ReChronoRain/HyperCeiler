package com.sevtinge.cemiuiler.module.hook.systemsettings

import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.LazyClass.SettingsFeaturesClass

class EnablePadArea : BaseHook() {
    override fun init() {
        setStaticObject(
            SettingsFeaturesClass,
            "IS_SUPPORT_TABLET_SCREEN_SETTINGS",
            true
        )
    }
}
