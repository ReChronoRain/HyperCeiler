package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.hook.aod.UnlockAlwaysOnDisplay;
import com.sevtinge.hyperceiler.module.base.BaseModule;

public class Aod extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(UnlockAlwaysOnDisplay.INSTANCE, mPrefsMap.getBoolean("aod_unlock_always_on_display"));
    }
}
