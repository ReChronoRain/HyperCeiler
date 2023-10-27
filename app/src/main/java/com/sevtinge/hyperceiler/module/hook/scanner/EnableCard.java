package com.sevtinge.hyperceiler.module.hook.scanner;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class EnableCard extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isAddBusinessCard", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isBusinessCardModuleAvailable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}






