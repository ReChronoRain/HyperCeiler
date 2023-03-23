package com.sevtinge.cemiuiler.module.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockHandGesture extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.data.data.runing.DataItemRunning", "supportHandGesture", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
