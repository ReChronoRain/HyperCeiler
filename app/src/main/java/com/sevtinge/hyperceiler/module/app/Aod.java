package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.hook.aod.UnlockAlwaysOnDisplay;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.aod.UnlockAodAon;

public class Aod extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(UnlockAlwaysOnDisplay.INSTANCE, mPrefsMap.getBoolean("aod_unlock_always_on_display"));
        initHook(new UnlockAodAon(), mPrefsMap.getBoolean("aod_unlock_aon"));
    }
}
