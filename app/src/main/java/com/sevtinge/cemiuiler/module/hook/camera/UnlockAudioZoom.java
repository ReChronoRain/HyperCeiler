package com.sevtinge.cemiuiler.module.hook.camera;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class UnlockAudioZoom extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.camera.aiaudio.AiAudioParameterManager", "isSupportAiAudioNew", new BaseHook.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
