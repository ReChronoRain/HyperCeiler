package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.misound.IncreaseSamplingRate;

public class MiSound extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(IncreaseSamplingRate.INSTANCE, mPrefsMap.getBoolean("misound_increase_sampling_rate"));
    }
}
