/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.CustomRefreshRate;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.DisableGetDisplayCtrlCode;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.DontKillApps;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.GmsDozeFix;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.LockMaxFps;
import com.sevtinge.hyperceiler.module.hook.powerkeeper.PreventBatteryWitelist;

@HookBase(targetPackage = "com.miui.powerkeeper",  isPad = false)
public class PowerKeeper extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new GmsDozeFix(), mPrefsMap.getBoolean("powerkeeper_gms_doze_fix"));
        initHook(new CustomRefreshRate(), mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(new DisableGetDisplayCtrlCode(), mPrefsMap.getBoolean("powerkeeper_disable_get_display_ctrl_code"));
        initHook(LockMaxFps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_lock_max_fps"));
        initHook(DontKillApps.INSTANCE, mPrefsMap.getBoolean("powerkeeper_do_not_kill_apps"));
        initHook(new PreventBatteryWitelist(), mPrefsMap.getBoolean("powerkeeper_prevent_recovery_of_battery_optimization_whitelist"));
    }
}
