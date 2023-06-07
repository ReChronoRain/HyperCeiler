package com.sevtinge.cemiuiler.module.systemsettings.aiimage;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockMemc extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isMemcSupport", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
    }
}
