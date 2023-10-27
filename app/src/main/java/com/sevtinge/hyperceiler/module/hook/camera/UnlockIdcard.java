package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockIdcard extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.idcard.IDCardModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
