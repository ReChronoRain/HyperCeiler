package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.powerkeeper.CustomRefreshRate;
import com.sevtinge.cemiuiler.module.powerkeeper.LockMaxFps;

public class PowerKeeper extends BaseModule {

    @Override
    public void handleLoadPackage() {

        initHook(new CustomRefreshRate(), mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(LockMaxFps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_lock_max_fps"));
    }
}
