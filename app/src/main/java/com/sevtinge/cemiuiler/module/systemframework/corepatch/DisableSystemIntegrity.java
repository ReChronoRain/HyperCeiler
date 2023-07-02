package com.sevtinge.cemiuiler.module.systemframework.corepatch;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class DisableSystemIntegrity extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("android.util.apk.ApkSignatureVerifier", "getMinimumSignatureSchemeVersionForTargetSdk", int.class, XC_MethodReplacement.returnConstant(1));
    }
}
