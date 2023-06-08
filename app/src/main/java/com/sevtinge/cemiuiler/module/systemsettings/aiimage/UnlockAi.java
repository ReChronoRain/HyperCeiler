package com.sevtinge.cemiuiler.module.systemsettings.aiimage;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockAi extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isAiSupport", Context.class, new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
    }
}
