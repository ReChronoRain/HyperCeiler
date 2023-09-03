package com.sevtinge.cemiuiler.module.hook.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockCvlens extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.CameraSettings", "isSupportCvLensDevice", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        try {
            findAndHookMethod("com.android.camera.CameraSettings", "getCvLensVersion", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(2);
                }
            });
            findAndHookMethod("com.android.camera2.CameraCapabilities", "getCvLensVersion", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(2);
                }
            });
            hookAllMethods("com.android.camera2.CameraCapabilitiesUtil", "getCvLensVersion", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(2);
                }
            });
        } catch (Exception e) {
            log("try to hook CvLensVersion failed!\n" + e);
            throw new RuntimeException(e);
        }
    }
}
