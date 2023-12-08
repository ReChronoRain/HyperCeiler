package com.sevtinge.hyperceiler.module.hook.misettings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.isFinal
import com.github.kyuubiran.ezxhelper.MemberExtensions.isStatic
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

object CustomRefreshRate : BaseHook() {
    private val resultMethod by lazy {
        dexKitBridge.findMethod {
           matcher {
               addUsingStringsEquals("btn_preferce_category")
           }
        }.single().getMethodInstance(EzXHelper.safeClassLoader)
    }
    override fun init() {
        val resultClass = loadClass("com.xiaomi.misettings.display.RefreshRate.RefreshRateActivity")

        resultMethod.createHook {
            before {
                it.args[0] = true
            }
        }

        resultClass.declaredFields.first { field ->
            field.isFinal && field.isStatic
        }.apply { isAccessible = true }.set(null, true)
    }
}
