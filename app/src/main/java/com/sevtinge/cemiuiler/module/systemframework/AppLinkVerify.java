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
            hookAllMethods("com.android.server.pm.verify.domain.DomainVerificationUtils", "isDomainVerificationIntent", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            param.setResult(false);
                        }
                    }
            );
        } catch (Throwable t) {
            log("Hook failed by: " + t);
        }
    }
}
