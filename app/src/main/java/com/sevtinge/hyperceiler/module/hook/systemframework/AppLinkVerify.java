package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;


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
            logE(TAG, this.lpparam.packageName, t);
        }
    }
}
