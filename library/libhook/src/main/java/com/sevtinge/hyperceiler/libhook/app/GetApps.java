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
import com.sevtinge.hyperceiler.libhook.rules.getapps.BypassRiskCheck;
import com.sevtinge.hyperceiler.libhook.rules.getapps.DeviceModify;
import com.sevtinge.hyperceiler.libhook.rules.getapps.DisableAds;
import com.sevtinge.hyperceiler.libhook.rules.getapps.DisablePackageMonitor;
import com.sevtinge.hyperceiler.libhook.rules.getapps.DisableStartPushDialog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.xiaomi.market")
public class GetApps extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new BypassRiskCheck(), PrefsBridge.getBoolean("market_bypass_risk_check"));
        initHook(new DisableAds(), PrefsBridge.getBoolean("market_disable_ads"));
        initHook(new DeviceModify(), PrefsBridge.getStringAsInt("market_device_modify_new", 0) != 0);

        initHook(DisablePackageMonitor.INSTANCE, PrefsBridge.getBoolean("market_package_monitor"));
        initHook(DisableStartPushDialog.INSTANCE, PrefsBridge.getBoolean("market_disable_start_push_dialog"));
    }
}
