package com.sevtinge.hyperceiler.module.hook.securitycenter.battery

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

object ScreenUsedTime : BaseHook() {
    private val cls by lazy {
        dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("not support screenPowerSplit", "PowerRankHelperHolder")
            }
        }.single().getInstance(EzXHelper.classLoader)
    }
    private val method1 by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("ishtar", "nuwa", "fuxi")
            }
        }.single().getMethodInstance(EzXHelper.classLoader)
    }
    private val method2 by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass = cls.name
                returnType = "boolean"
                // paramTypes = listOf() 2.0.0-rc3 已经修复此错误，可以使用
                paramCount = 0
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    override fun init() {
        Log.i("methods2 :$method2")
        method2.forEach {
            it.createHook {
                returnConstant(
                    when (it) {
                        method1 -> true
                        else -> false
                    }
                )
            }
        }
    }
}
