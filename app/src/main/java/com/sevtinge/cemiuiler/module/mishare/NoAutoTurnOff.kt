package com.sevtinge.cemiuiler.module.mishare

import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper.classLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object NoAutoTurnOff : BaseHook() {
    override fun init() {
        val result1 = MiShareDexKit.mMiShareResultMethodsMap!!["qwq"]
        val result2 = MiShareDexKit.mMiShareResultClassMap!!["qwq2"]

        // 禁用小米互传功能自动关闭部分
        result1!!.map {
            it.getMethodInstance(lpparam.classLoader)
        }.createHooks {
            before {
                it.result = null
            }
        }

        // 干掉小米互传十分钟倒计时 Toast
        result2!!.map {
            it.getClassInstance(classLoader).methodFinder()
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
