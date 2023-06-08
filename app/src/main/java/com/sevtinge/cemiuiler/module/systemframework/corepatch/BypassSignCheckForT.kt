package com.sevtinge.cemiuiler.module.systemframework.corepatch

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object BypassSignCheckForT : BaseHook() {
    override fun init() {
        try {
            loadClass("android.util.apk.ApkSignatureVerifier").methodFinder().first {
                name == "getMinimumSignatureSchemeVersionForTargetSdk"
            }.createHook {
                after { param ->
                    param.result = 1
                }
            }
        } catch (e: Throwable) {
            logE(e)
        }
    }
}
