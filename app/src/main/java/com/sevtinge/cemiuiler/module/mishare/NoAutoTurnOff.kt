package com.sevtinge.cemiuiler.module.mishare

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.mishare.MiShareDexKit.mMiShareResultMethodsMap

class NoAutoTurnOff : BaseHook() {

    override fun init() {
        try {
            val result = mMiShareResultMethodsMap["qwq"]!!
            for (descriptor in result) {
                try {
                    log("descriptor method is $descriptor")
                    val autoOff = descriptor.getMethodInstance(lpparam.classLoader)
                    log("autoOff method is $autoOff")
                    autoOff.hookBefore {
                        it.result = null
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (e: Throwable) {
            logE(e)
        }
        try {
            val result = mMiShareResultMethodsMap["qwq2"]!!
            for (descriptor in result) {
                try {
                    val securityAgree = descriptor.getMethodInstance(lpparam.classLoader)
                    log("securityAgree method is $securityAgree")
                    securityAgree.hookBefore { param ->
                        if (param.args[1].equals("security_agree")) {
                            param.result = false
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (e: Throwable) {
            logE(e)
        }
    }
}