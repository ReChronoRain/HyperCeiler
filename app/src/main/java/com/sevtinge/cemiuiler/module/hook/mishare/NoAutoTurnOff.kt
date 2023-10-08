package com.sevtinge.cemiuiler.module.hook.mishare

import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.addUsingStringsEquals
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge

object NoAutoTurnOff : BaseHook() {
    private val nullMethod by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingStringsEquals("EnabledState", "mishare_enabled")
            }
        }.map { it.getMethodInstance(classLoader) }.toList()
    }

    private val toastClass by lazy {
        dexKitBridge.findClass {
            matcher {
                addUsingStringsEquals("null context", "cta_agree")
            }
        }.map { it.getInstance(classLoader) }.toList()
    }

    override fun init() {
        // 禁用小米互传功能自动关闭部分
        nullMethod.createHooks {
            before {
                it.result = null
            }
        }

        // 干掉小米互传十分钟倒计时 Toast
        toastClass.forEach {
            it.methodFinder()
                .filterByReturnType(Boolean::class.java)
                .filterByParamCount(2)
                .filterByParamTypes(Context::class.java, String::class.java)
                .toList().createHooks {
                    before { param ->
                        if (param.args[1].equals("security_agree")) {
                            param.result = false
                        }
                    }
                }
        }
    }
}
