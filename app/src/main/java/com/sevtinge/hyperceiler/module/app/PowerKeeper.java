package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.CustomRefreshRate;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.DontKillApps;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.LockMaxFps;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.PreventBatteryWitelist;

public class PowerKeeper extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(CustomRefreshRate.INSTANCE, mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(LockMaxFps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_lock_max_fps"));
        initHook(DontKillApps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_do_not_kill_apps"));
        initHook(new PreventBatteryWitelist(), mPrefsMap.getBoolean("powerkeeper_prevent_recovery_of_battery_optimization_whitelist"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
