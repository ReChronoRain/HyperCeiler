package com.sevtinge.cemiuiler.module.hook.scanner.document;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class EnableExcel extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.xiaomi.scanner.util.SPUtils", "getFormModule", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isSupportForm", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookAllMethods("com.xiaomi.scanner.settings.FeatureManager", "isAddFormRecognitionFunction", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}


