package com.sevtinge.hyperceiler.module.hook.systemframework.corepatch

import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils

object BypassSignCheckForT : BaseHook() {
    override fun init() {
        try {
            hookAllMethods("android.util.apk.ApkSignatureVerifier", "getMinimumSignatureSchemeVersionForTargetSdk", object : MethodHook() {
                override fun after(param: MethodHookParam?) {
                    param?.result = 1
                }
            })
        } catch (e: Throwable) {
            XposedLogUtils.logE(TAG, this.lpparam.packageName, e)
        }
    }
}
