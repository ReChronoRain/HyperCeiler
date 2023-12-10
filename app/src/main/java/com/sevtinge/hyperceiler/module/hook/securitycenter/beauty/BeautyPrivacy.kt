package com.sevtinge.hyperceiler.module.hook.securitycenter.beauty

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge
object BeautyPrivacy : BaseHook() {
    private val privateCls by lazy {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("persist.sys.privacy_camera")
            }
        }.single().getInstance(EzXHelper.safeClassLoader)
    }

    private val R0 by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("persist.sys.privacy_camera")
            }
        }.single().getMethodInstance(EzXHelper.safeClassLoader)
    }

    private val invokeMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                declaredClass = privateCls.name
                paramTypes = emptyList()
                returnType = "boolean"
                addInvoke {
                    returnType = R0.returnType.name
                    paramTypes = listOf(R0.parameterTypes[0].name)
                    declaredClass = privateCls.name
                }
            }
        }.map { it.getMethodInstance(EzXHelper.classLoader) }.toList()
    }

    override fun init() {
        R0.createHook {
            before {
                it.args[0] = true
            }
        }

        invokeMethod.createHooks {
            returnConstant(true)
        }
    }
}
