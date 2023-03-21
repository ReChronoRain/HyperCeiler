package com.sevtinge.cemiuiler.module.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockAiWatermark extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.aiwatermark.AIWaterModuleEntry", "support", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}