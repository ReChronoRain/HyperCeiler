package com.sevtinge.cemiuiler.module.systemframework;

import android.content.Intent;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AppLinkVerify extends BaseHook {

    @Override
    public void init() {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.server.pm.verify.domain.DomainVerificationUtils",
                    lpparam.classLoader,
                    "isDomainVerificationIntent",
                    Intent.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(false);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
