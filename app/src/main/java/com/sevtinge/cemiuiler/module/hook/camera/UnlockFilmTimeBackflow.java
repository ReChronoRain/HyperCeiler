package com.sevtinge.cemiuiler.module.hook.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockFilmTimeBackflow extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.film.timebackflow.TimeBackflowModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
