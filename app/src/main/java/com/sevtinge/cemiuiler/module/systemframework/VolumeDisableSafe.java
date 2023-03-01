package com.sevtinge.cemiuiler.module.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class VolumeDisableSafe extends BaseHook {

    Class<?> mAudioService;

    @Override
    public void init() {
        mAudioService = findClassIfExists("com.android.server.audio.AudioService");
        returnIntConstant(mAudioService, "safeMediaVolumeIndex");
    }

    private void returnIntConstant(Class<?> cls, String methodName) {
        hookAllMethods(cls, methodName, XC_MethodReplacement.returnConstant(2147483646));
    }
}
