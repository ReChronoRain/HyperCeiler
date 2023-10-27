package com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage;

import com.sevtinge.hyperceiler.module.base.BaseHook;

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
