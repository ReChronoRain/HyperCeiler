/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.various.system.DisableSystemAds;
import com.sevtinge.hyperceiler.libhook.rules.various.system.DisableSystemTelemetry;

@HookBase(targetPackage = "com.mi.android.globalFileexplorer")
public class GlobalFileExplorer extends BaseLoad {
    @Override public void onPackageLoaded() {
        initHook(new DisableSystemAds(), PrefsBridge.getBoolean("various_disable_system_ads"));
        initHook(new DisableSystemTelemetry(), PrefsBridge.getBoolean("various_disable_system_telemetry"));
    }
}
