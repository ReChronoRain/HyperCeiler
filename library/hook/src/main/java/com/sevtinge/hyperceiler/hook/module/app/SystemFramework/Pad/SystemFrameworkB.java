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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.app.SystemFramework.Pad;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisableLowApiCheckForU;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisableMiuiWatermark;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisablePersistent;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisableThermal;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.FlagSecure;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.ThermalBrightness;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch.AllowUpdateSystemApp;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch.BypassIsolationViolation;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch.BypassSignCheckForT;

@HookBase(targetPackage = "android", isPad = 1, targetSdk = 36)
public class SystemFrameworkB extends BaseModule {

    @Override
    public void handleLoadPackage() {

        // 核心破解
        initHook(BypassSignCheckForT.INSTANCE, mPrefsMap.getBoolean("system_framework_core_patch_auth_creak") || mPrefsMap.getBoolean("system_framework_core_patch_disable_integrity"));
        initHook(new BypassIsolationViolation(), mPrefsMap.getBoolean("system_framework_core_patch_bypass_isolation_violation"));
        initHook(new AllowUpdateSystemApp(), mPrefsMap.getBoolean("system_framework_core_patch_allow_update_system_app"));
        initHook(new DisableLowApiCheckForU(), mPrefsMap.getBoolean("system_framework_disable_low_api_check"));
        initHook(new DisablePersistent(), mPrefsMap.getBoolean("system_framework_disable_persistent"));

        // 其它-显示与通知
        initHook(new FlagSecure(), mPrefsMap.getBoolean("system_other_flag_secure"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));

        // 其它-底层
        initHook(DisableThermal.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_thermal"));
        initHook(new ThermalBrightness(), mPrefsMap.getBoolean("system_framework_other_thermal_brightness"));
        initHook(new DisableMiuiWatermark(), mPrefsMap.getBoolean("system_framework_disable_miui_watermark"));
    }
}
