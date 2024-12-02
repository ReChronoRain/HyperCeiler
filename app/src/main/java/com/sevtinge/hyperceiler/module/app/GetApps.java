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
import com.sevtinge.hyperceiler.module.hook.getapps.BypassRiskCheck;
import com.sevtinge.hyperceiler.module.hook.getapps.DeviceModify;
import com.sevtinge.hyperceiler.module.hook.getapps.DisableAds;
import com.sevtinge.hyperceiler.module.hook.getapps.DisablePackageMonitor;

@HookBase(targetPackage = "com.xiaomi.market", isPad = false)
public class GetApps extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new BypassRiskCheck(), mPrefsMap.getBoolean("market_bypass_risk_check"));
        initHook(new DisableAds(), mPrefsMap.getBoolean("market_disable_ads"));
        initHook(new DeviceModify(), mPrefsMap.getStringAsInt("market_device_modify_new", 0) != 0);

        initHook(DisablePackageMonitor.INSTANCE, mPrefsMap.getBoolean("market_package_monitor"));
    }
}
