package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.CustomRefreshRate;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.DisableGetDisplayCtrlCode;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.DontKillApps;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.LockMaxFps;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.PreventBatteryWitelist;

public class PowerKeeper extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(CustomRefreshRate.INSTANCE, mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(new DisableGetDisplayCtrlCode(), mPrefsMap.getBoolean("powerkeeper_disable_get_display_ctrl_code"));
        initHook(LockMaxFps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_lock_max_fps"));
        initHook(DontKillApps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_do_not_kill_apps"));
        initHook(new PreventBatteryWitelist(), mPrefsMap.getBoolean("powerkeeper_prevent_recovery_of_battery_optimization_whitelist"));
    }
}
