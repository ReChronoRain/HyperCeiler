package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableVerifyCanBeDisabled extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.server.pm.PackageManagerServiceImpl", "canBeDisabled", String.class, int.class, new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
