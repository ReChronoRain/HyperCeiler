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
package com.sevtinge.hyperceiler.hook.module.app.SystemFramework.Phone;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.AutoEffectSwitchForSystem;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.ConservativeMilletFramework;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisableLowApiCheckForB;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisableMiuiWatermark;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisablePersistent;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisableRemoveFingerprintSensorConfig;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.DisableThermal;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.EffectBinderProxy;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.FlagSecure;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.GMSDozeFixFramework;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.PackagePermissions;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.ThermalBrightness;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch.AllowUpdateSystemApp;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch.BypassIsolationViolation;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch.BypassSignCheckForT;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.display.DisplayCutout;
import com.sevtinge.hyperceiler.hook.module.skip.GlobalActions;

@HookBase(targetPackage = "android", isPad = 2, targetSdk = 36)
public class SystemFrameworkB extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // 核心破解
        initHook(BypassSignCheckForT.INSTANCE,
            (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak") || mPrefsMap.getBoolean("system_framework_core_patch_disable_integrity"))
            && mPrefsMap.getBoolean("system_framework_core_patch_enable")
        );
        initHook(new BypassIsolationViolation(), mPrefsMap.getBoolean("system_framework_core_patch_bypass_isolation_violation"));
        initHook(new AllowUpdateSystemApp(), mPrefsMap.getBoolean("system_framework_core_patch_allow_update_system_app"));
        initHook(new DisableLowApiCheckForB(), mPrefsMap.getBoolean("system_framework_disable_low_api_check"));
        initHook(new DisablePersistent(), mPrefsMap.getBoolean("system_framework_disable_persistent"));

        // 修复 A16 移植包开启核心破解后掉指纹，仅作备选项
        initHook(DisableRemoveFingerprintSensorConfig.INSTANCE, mPrefsMap.getBoolean("system_framework_core_patch_unloss_fingerprint"));

        // 手势初始化
        initHook(new PackagePermissions(), true);
        initHook(new GlobalActions(), true);

        // 显示
        initHook(DisplayCutout.INSTANCE, mPrefsMap.getBoolean("system_ui_display_hide_cutout_enable"));

        // 其它-显示与通知
        initHook(new FlagSecure(), mPrefsMap.getBoolean("system_other_flag_secure"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));

        // 其它-底层
        initHook(DisableThermal.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_thermal"));
        initHook(new ThermalBrightness(), mPrefsMap.getBoolean("system_framework_other_thermal_brightness"));
        initHook(new DisableMiuiWatermark(), mPrefsMap.getBoolean("system_framework_disable_miui_watermark"));
        initHook(ConservativeMilletFramework.INSTANCE, mPrefsMap.getBoolean("powerkeeper_conservative_millet"));
        initHook(GMSDozeFixFramework.INSTANCE, mPrefsMap.getBoolean("powerkeeper_gms_doze_fix"));

        initHook(new PackagePermissions(), true);
        initHook(new GlobalActions(), true);
        if (mPrefsMap.getBoolean("misound_bluetooth") && isHyperOSVersion(2f)) {
            initHook(new EffectBinderProxy());
            initHook(new AutoEffectSwitchForSystem());
        }
    }
}
