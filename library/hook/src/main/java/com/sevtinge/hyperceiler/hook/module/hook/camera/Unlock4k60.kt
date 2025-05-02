package com.sevtinge.hyperceiler.hook.module.hook.camera

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import java.lang.reflect.Method

object Unlock4k60 : BaseHook() {
    private val isMethod by lazy<Method> {
        DexKit.findMember("4k60") {
            it.findMethod {
                matcher {
                    paramCount = 3
                    returnType = "boolean"
                    usingNumbers(60)
                    addInvoke("Ljava/util/Iterator;->hasNext()Z")
                }
            }.single()
        }
    }

    override fun init() {
        isMethod.createHook {
            returnConstant(true)
        }
    }
}