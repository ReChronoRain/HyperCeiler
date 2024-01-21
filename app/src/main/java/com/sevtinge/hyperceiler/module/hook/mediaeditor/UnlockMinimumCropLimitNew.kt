package com.sevtinge.hyperceiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier

object UnlockMinimumCropLimitNew : BaseHook() {
    private val mScreenCropViewMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("not in bound")
                }
                usingNumbers(0.5f, 200)
                returnType = "int"
                modifiers = Modifier.FINAL
            }
            // 老版本匹配
            matcher {
                declaredClass {
                    addUsingStringsEquals("fixImageBounds %f,%f")
                }
                usingNumbers(0.5f, 200)
                returnType = "int"
                modifiers = Modifier.FINAL
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    override fun init() {
        mScreenCropViewMethod.createHooks {
            returnConstant(0)
        }
    }
}
