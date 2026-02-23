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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.powerkeeper.ConservativeMillet;
import com.sevtinge.hyperceiler.libhook.rules.powerkeeper.CustomRefreshRate;
import com.sevtinge.hyperceiler.libhook.rules.powerkeeper.DisableGetDisplayCtrlCode;
import com.sevtinge.hyperceiler.libhook.rules.powerkeeper.DontKillApps;
import com.sevtinge.hyperceiler.libhook.rules.powerkeeper.GmsDozeFix;
import com.sevtinge.hyperceiler.libhook.rules.powerkeeper.LockMaxFps;
import com.sevtinge.hyperceiler.libhook.rules.powerkeeper.PreventBatteryWitelist;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.miui.powerkeeper")
public class PowerKeeper extends BaseLoad {

    public PowerKeeper() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        initHook(new GmsDozeFix(), PrefsBridge.getBoolean("powerkeeper_gms_doze_fix"));
        initHook(new ConservativeMillet(), PrefsBridge.getBoolean("powerkeeper_conservative_millet"));
        initHook(new CustomRefreshRate(), PrefsBridge.getBoolean("various_custom_refresh_rate"));
        initHook(new DisableGetDisplayCtrlCode(), PrefsBridge.getBoolean("powerkeeper_disable_get_display_ctrl_code"));
        initHook(LockMaxFps.INSTANCE, PrefsBridge.getBoolean("powerkeeper_lock_max_fps"));
        initHook(DontKillApps.INSTANCE, PrefsBridge.getBoolean("powerkeeper_do_not_kill_apps"));
        initHook(new PreventBatteryWitelist(), PrefsBridge.getBoolean("powerkeeper_prevent_recovery_of_battery_optimization_whitelist"));
    }
}
