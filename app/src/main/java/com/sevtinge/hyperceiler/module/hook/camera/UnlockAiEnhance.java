package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockAiEnhance extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera2.CameraCapabilities", "supportAiEnhancedVideo", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookAllMethods("com.android.camera2.CameraCapabilitiesUtil", "supportAiEnhancedVideo", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
