package com.sevtinge.cemiuiler.module.hook.mediaeditor

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import java.lang.reflect.Modifier

object UnlockMinimumCropLimitNew : BaseHook() {
    private val mScreenCropViewMethodToNew by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("not in bound")
                }
                usingNumbers(0.5f, 200)
                returnType = "int"
                modifiers = Modifier.FINAL
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    private val mScreenCropViewMethodToOld by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                    addUsingStringsEquals("fixImageBounds %f,%f")
                }
                usingNumbers(0.5f, 200)
                returnType = "int"
                modifiers = Modifier.FINAL
            }
        }.firstOrNull()?.getMethodInstance(EzXHelper.safeClassLoader)
    }

    override fun init() {
        mScreenCropViewMethodToNew.createHooks {
            returnConstant(0)
        }
        if (mScreenCropViewMethodToOld != null) {
            mScreenCropViewMethodToOld!!.createHook {
                returnConstant(0)
            }
        }
    }
}
