package com.sevtinge.hyperceiler.module.hook.scanner.document;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class EnableDocument extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isAddDocumentModule", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}






