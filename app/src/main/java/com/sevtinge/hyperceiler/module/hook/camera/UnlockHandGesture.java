package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockHandGesture extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.camera.data.data.runing.DataItemRunning", "supportHandGesture", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
