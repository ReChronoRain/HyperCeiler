package com.sevtinge.hyperceiler.module.hook.milink;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AllowCameraDevices extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.xiaomi.vtcamera.cloud.RulesConfig", "isDeviceAllowed", String.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(true);
                }
            }
        );
    }
}
