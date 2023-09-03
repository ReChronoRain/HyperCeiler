package com.sevtinge.cemiuiler.module.hook.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockMenMakeup extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera2.CameraCapabilities", "isSupportMaleMakeupSwitch", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookAllMethods("com.android.camera2.CameraCapabilitiesUtil", "isSupportMaleMakeupSwitch", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
