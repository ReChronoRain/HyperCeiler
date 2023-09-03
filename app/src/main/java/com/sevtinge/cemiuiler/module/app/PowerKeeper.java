package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.powerkeeper.CustomRefreshRate;
import com.sevtinge.cemiuiler.module.hook.powerkeeper.DontKillApps;
import com.sevtinge.cemiuiler.module.hook.powerkeeper.LockMaxFps;
import com.sevtinge.cemiuiler.module.hook.powerkeeper.PowerKeeperDexKit;

public class PowerKeeper extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new PowerKeeperDexKit());
        initHook(CustomRefreshRate.INSTANCE, mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(LockMaxFps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_lock_max_fps"));
        initHook(DontKillApps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_do_not_kill_apps"));
    }
}
