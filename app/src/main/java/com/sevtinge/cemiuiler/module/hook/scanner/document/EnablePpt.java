package com.sevtinge.cemiuiler.module.hook.scanner.document;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class EnablePpt extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isSupportPpt", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

    }
}




