package com.sevtinge.cemiuiler.module.systemframework.corepatch

import com.sevtinge.cemiuiler.module.base.BaseHook

object BypassSignCheckForT : BaseHook() {
    override fun init() {
        hookAllMethods("android.util.apk.ApkSignatureVerifier", "getMinimumSignatureSchemeVersionForTargetSdk",
            object : MethodHook() { override fun after(param: MethodHookParam?) { param?.result = 1 }}
        )
        hookAllMethods("com.android.server.pm.InstallPackageHelper ", "doesSignatureMatchForPermissions",
            object : MethodHook() { override fun before(param: MethodHookParam?) { param?.result = true }}
        )
    }
}
