package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.camera.*;

public class Camera extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockAiWatermark(), mPrefsMap.getBoolean("camera_features_unlock_aiwatermark"));
        initHook(new UnlockAmbilight(), mPrefsMap.getBoolean("camera_features_unlock_ambilight"));
        initHook(new UnlockClone(), mPrefsMap.getBoolean("camera_features_unlock_clone"));
    }
}
