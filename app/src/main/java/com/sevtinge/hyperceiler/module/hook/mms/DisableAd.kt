package com.sevtinge.hyperceiler.module.hook.mms

import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier


object DisableAd : BaseHook() {
    override fun init() {
        try {
            dexKitBridge.findMethod {
                matcher {
                    declaredClass {
                        addUsingStringsEquals("Unknown type of the message: ")
                    }
                    usingNumbers(3, 4)
                    modifiers = Modifier.FINAL
                    returnType = "boolean"
                    paramCount = 0
                }
            }.map { it.getMethodInstance(EzXHelper.safeClassLoader) }.createHooks {
                returnConstant(false)
            }
        } catch (e: Throwable) {
            logE(TAG, this.lpparam.packageName, e)
        }
        findAndHookMethod("com.miui.smsextra.ui.BottomMenu", "allowMenuMode",
            Context::class.java, object : MethodHook() {
                @Throws(Throwable::class)
                override fun before(param: MethodHookParam) {
                    param.setResult(false)
                }
            })
    }
}
