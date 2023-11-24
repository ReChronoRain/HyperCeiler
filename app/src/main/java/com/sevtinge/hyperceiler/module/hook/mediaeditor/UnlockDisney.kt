package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier

object UnlockDisney : BaseHook() {
    override fun init() {
        val disney = dexKitBridge.findMethod {
            matcher {
                addCall {
                    addUsingStringsEquals("magic_recycler_matting_0", "magic_recycler_clear_icon")
                    returnType = "java.util.List"
                    paramCount = 0
                }
                modifiers = Modifier.STATIC
                returnType = "boolean"
                paramCount = 0
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()

        // debug 用
        for (d in disney) {
            logI(TAG,"disney name is $d")
        }
        disney.createHooks {
            returnConstant(true)
        }
    }
}
