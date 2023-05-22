package com.sevtinge.cemiuiler.module.systemframework.corepatch

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookMethod
import com.sevtinge.cemiuiler.module.base.BaseHook

object BypassSignCheckForT : BaseHook() {
    override fun init() {
        try {
            findMethod("android.util.apk.ApkSignatureVerifier") {
                name == "getMinimumSignatureSchemeVersionForTargetSdk"
            }.hookMethod {
                after { param ->
                    param.result = 1
                }
            }
            /*findAndHookMethod(
                "android.util.apk.ApkSignatureVerifier",
                "getMinimumSignatureSchemeVersionForTargetSdk",
                object : MethodHook() {
                    override fun after(param: MethodHookParam?) {
                        param?.result = 1
                    }
                }
            )*/
        } catch (e: Throwable) {
            log("hook failed by $e")
        }

    }

}