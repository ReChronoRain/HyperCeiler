package com.sevtinge.hyperceiler.module.hook.contentextension

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.api.IS_TABLET

object UnlockTaplus : BaseHook() {
    override fun init() {
        if (!IS_TABLET) return
        loadClass("com.miui.contentextension.setting.activity.MainSettingsActivity").methodFinder()
            .first {
                name == "getFragment"
            }.createHook {
                setStaticObject(
                    loadClass("miui.os.Build"), "IS_TABLET", false
                )
            }
    }
}
