package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;


public class UnlockCvlens extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.camera.CameraSettings", "isSupportCvLensDevice", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        try {
            hookAllMethods("com.android.camera.CameraSettings", "getCvLensVersion", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(2);
                }
            });
            hookAllMethods("com.android.camera2.CameraCapabilities", "getCvLensVersion", new MethodHook() {
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
            logE(TAG, this.lpparam.packageName, "try to hook CvLensVersion failed" + e);
            throw new RuntimeException(e);
        }
    }
}
