package com.sevtinge.cemiuiler.module.hook.systemframework.corepatch

import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.log.XposedLogUtils

object BypassSignCheckForT : BaseHook() {
    override fun init() {
        try {
            hookAllMethods("android.util.apk.ApkSignatureVerifier", "getMinimumSignatureSchemeVersionForTargetSdk", object : MethodHook() {
                override fun after(param: MethodHookParam?) {
                    param?.result = 1
                }
            })
        } catch (e: Throwable) {
            XposedLogUtils.logE(TAG, e)
        }
    }
}
