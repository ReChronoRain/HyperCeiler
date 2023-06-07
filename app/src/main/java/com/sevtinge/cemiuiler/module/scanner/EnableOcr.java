package com.sevtinge.cemiuiler.module.scanner;

import com.sevtinge.cemiuiler.module.base.BaseHook;

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




