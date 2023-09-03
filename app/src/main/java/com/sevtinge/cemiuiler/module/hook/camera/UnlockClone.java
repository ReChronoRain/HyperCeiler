package com.sevtinge.cemiuiler.module.hook.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockClone extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.clone.CloneModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.camera.features.mode.more.clone.MoreCloneModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
