package com.sevtinge.cemiuiler.module.hook.scanner;

import com.sevtinge.cemiuiler.module.base.BaseHook;

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

