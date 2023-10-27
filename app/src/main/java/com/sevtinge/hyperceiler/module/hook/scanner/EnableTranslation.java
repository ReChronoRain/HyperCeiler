package com.sevtinge.hyperceiler.module.hook.scanner;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class EnableTranslation extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isAddTranslation", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isTranslationModuleAvailable", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}






