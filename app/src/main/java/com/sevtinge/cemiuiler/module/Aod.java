package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.aod.UnlockAlwaysOnDisplay;
import com.sevtinge.cemiuiler.module.base.BaseModule;

public class Aod extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(UnlockAlwaysOnDisplay.INSTANCE, mPrefsMap.getBoolean("aod_unlock_always_on_display"));
    }
}
