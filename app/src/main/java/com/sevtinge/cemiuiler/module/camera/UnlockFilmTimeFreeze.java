package com.sevtinge.cemiuiler.module.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockFilmTimeFreeze extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.film.timefreeze.TimeFreezeModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}