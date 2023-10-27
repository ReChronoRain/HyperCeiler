package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockVideosky extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.videosky.VideoSkyModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
