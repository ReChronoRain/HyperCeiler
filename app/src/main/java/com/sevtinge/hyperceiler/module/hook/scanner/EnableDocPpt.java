package com.sevtinge.hyperceiler.module.hook.scanner;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class EnableDocPpt extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isPPTModuleAvailable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}

