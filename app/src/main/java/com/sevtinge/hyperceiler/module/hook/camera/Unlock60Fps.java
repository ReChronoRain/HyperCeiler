package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class Unlock60Fps extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.camera.data.data.config.ComponentConfigVideoQuality", "isSupport60FPS", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
