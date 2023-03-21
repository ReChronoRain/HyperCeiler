package com.sevtinge.cemiuiler.module.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockDualcam extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.dualcam.DualCamModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
