package com.sevtinge.cemiuiler.module.hook.misettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.isFinal
import com.github.kyuubiran.ezxhelper.MemberExtensions.isStatic
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

object CustomRefreshRate : BaseHook() {
    private val resultMethod by lazy {
        dexKitBridge.findMethod {
           matcher {
               addUsingStringsEquals("btn_preferce_category")
           }
        }
    }
    override fun init() {
        /*val result1 = MiSettingsDexKit.mMiSettingsResultMethodsMap!!["category"]
        val result2 = MiSettingsDexKit.mMiSettingsResultClassMap!!["refresh"]*/
        val resultClass = loadClass("com.xiaomi.misettings.display.RefreshRate.RefreshRateActivity")

        resultMethod.first().getMethodInstance(EzXHelper.classLoader).createHook {
            before {
                it.args[0] = true
            }
        }

        /*resultClass.map {
            it.getClassInstance(EzXHelper.classLoader).fieldFinder()
                .toList().forEach { field ->
                    if (field.isFinal && field.isStatic) {
                        field.isAccessible = true
                        field.set(null, true)
                    }
                }
        }*/
        resultClass.declaredFields.first { field ->
            field.isFinal && field.isStatic
        }.apply { isAccessible = true }.set(null, true)
    }
}
