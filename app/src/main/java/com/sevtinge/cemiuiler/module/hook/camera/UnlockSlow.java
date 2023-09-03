package com.sevtinge.cemiuiler.module.hook.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockSlow extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.slow.SlowMotionModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
