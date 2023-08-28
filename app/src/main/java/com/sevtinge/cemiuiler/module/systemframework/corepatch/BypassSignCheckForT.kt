package com.sevtinge.cemiuiler.module.systemframework.corepatch

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

object BypassSignCheckForT : BaseHook() {
    override fun init() {
        try {
            loadClass("android.util.apk.ApkSignatureVerifier").methodFinder()
                .filterByName("getMinimumSignatureSchemeVersionForTargetSdk")
                .filterByParamTypes(Int::class.javaPrimitiveType)
                .toList().createHooks {
                returnConstant(1)
            }
        } catch (e: Throwable) {
            logE(e)
        }
    }
}
