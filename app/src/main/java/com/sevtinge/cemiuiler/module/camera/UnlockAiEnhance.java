package com.sevtinge.cemiuiler.module.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockAiEnhance extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera2.CameraCapabilities", "supportAiEnhancedVideo", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.camera2.CameraCapabilitiesUtil", "supportAiEnhancedVideo", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
