package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockAiWatermark extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.camera.features.mode.aiwatermark.AIWaterModuleEntry", "support", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
