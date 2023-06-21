package com.sevtinge.cemiuiler.module.contentextension

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.api.isPad

object UnlockTaplus : BaseHook() {
    override fun init() {
        loadClass("com.android.settings.utils.SettingsFeatures").methodFinder().first {
            name == "isNeedRemoveContentExtension"
        }.createHook {
            returnConstant(false)
        }

        if (isPad()) {
            loadClass("com.miui.contentextension.setting.activity.MainSettingsActivity").methodFinder()
                .first {
                    name == "getFragment"
                }.createHook {
                setStaticObject(
                    loadClass("miui.os.Build"),
                    "IS_TABLET",
                    false
                )
            }
        }
    }
}
