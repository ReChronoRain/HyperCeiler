package com.sevtinge.cemiuiler.module.scanner;

import com.sevtinge.cemiuiler.module.base.BaseHook;

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






