package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

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
            XposedLogUtils.logE(TAG, this.lpparam.packageName, "try to hook CvLensVersion failed" + e);
            throw new RuntimeException(e);
        }
    }
}
