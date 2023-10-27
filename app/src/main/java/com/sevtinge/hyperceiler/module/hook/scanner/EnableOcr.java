package com.sevtinge.hyperceiler.module.hook.scanner;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class EnableOcr extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isAddTextExtractionFunction", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}




