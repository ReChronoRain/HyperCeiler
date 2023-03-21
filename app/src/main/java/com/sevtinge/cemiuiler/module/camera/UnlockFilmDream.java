package com.sevtinge.cemiuiler.module.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockFilmDream extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.camera.features.mode.film.dream.DreamModuleEntry", "support", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}