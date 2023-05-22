package com.sevtinge.cemiuiler.module.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedBridge;

public class UnlockCvlens extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.CameraSettings", "isSupportCvLensDevice", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        try {
            findAndHookMethod("com.android.camera.CameraSettings", "getCvLensVersion", new BaseHook.MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(2);
                }
            });
            findAndHookMethod("com.android.camera2.CameraCapabilities", "getCvLensVersion", new BaseHook.MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(2);
                }
            });
            hookAllMethods("com.android.camera2.CameraCapabilitiesUtil", "getCvLensVersion", new BaseHook.MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(2);
                }
            });
            log("try to hook CvLensVersion success!");
        } catch (Exception e) {
            log("try to hook CvLensVersion failed!\n" + e);
            throw new RuntimeException(e);
        }
    }
}
