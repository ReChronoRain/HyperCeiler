package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.hook.aod.UnlockAlwaysOnDisplay;
import com.sevtinge.cemiuiler.module.base.BaseModule;

public class Aod extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(UnlockAlwaysOnDisplay.INSTANCE, mPrefsMap.getBoolean("aod_unlock_always_on_display"));
    }
}
