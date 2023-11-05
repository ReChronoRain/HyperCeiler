package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class VolumeDisableSafe extends BaseHook {

    Class<?> mAudioService;

    @Override
    public void init() {
        mAudioService = findClassIfExists("com.android.server.audio.AudioService");
        returnIntConstant(mAudioService, "safeMediaVolumeIndex");
    }

    private void returnIntConstant(Class<?> cls, String methodName) {
        hookAllMethods(cls, methodName, MethodHook.returnConstant(2147483646));
    }
}
