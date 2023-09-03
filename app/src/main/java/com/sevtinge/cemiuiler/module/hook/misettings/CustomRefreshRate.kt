package com.sevtinge.cemiuiler.module.hook.misettings

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.isFinal
import com.github.kyuubiran.ezxhelper.MemberExtensions.isStatic
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object CustomRefreshRate : BaseHook() {
    override fun init() {
        val result1 = MiSettingsDexKit.mMiSettingsResultMethodsMap!!["category"]
        val result2 = MiSettingsDexKit.mMiSettingsResultClassMap!!["refresh"]

        result1!!.single().getMethodInstance(EzXHelper.classLoader).createHook {
            before {
                it.args[0] = true
            }
        }

        result2!!.map {
            it.getClassInstance(EzXHelper.classLoader).fieldFinder()
                .toList().forEach { field ->
                    if (field.isFinal && field.isStatic) {
                        field.isAccessible = true
                        field.set(null, true)
                    }
                }
        }
    }
}
