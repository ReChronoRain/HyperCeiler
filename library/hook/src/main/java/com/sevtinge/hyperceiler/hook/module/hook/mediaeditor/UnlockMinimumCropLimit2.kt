package com.sevtinge.hyperceiler.hook.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import java.lang.reflect.*

object UnlockMinimumCropLimit2 : BaseHook() {
    override fun init() {
        DexKit.findMemberList<Method>("MinimumCropLimit2") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "int"
                    paramCount = 0
                    usingNumbers(0.5f) // 1.8 开始有一个方法去除了 200 这个参数，需要重找
                    modifiers = Modifier.FINAL

                    // 理论上适配 1.7 - 1.8+ 全版本
                    addInvoke("Ljava/lang/Math;->max(II)I")
                }
            }
        }.forEach { method ->
            method.createHook {
                returnConstant(0)
            }
        }
    }
}
