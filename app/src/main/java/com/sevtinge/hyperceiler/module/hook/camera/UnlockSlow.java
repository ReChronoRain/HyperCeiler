package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockSlow extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.camera.features.mode.slow.SlowMotionModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
