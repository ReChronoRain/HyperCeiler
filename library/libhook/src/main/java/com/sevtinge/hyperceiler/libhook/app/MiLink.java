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
import com.sevtinge.hyperceiler.libhook.rules.milink.AllowCameraDevices;
import com.sevtinge.hyperceiler.libhook.rules.milink.FuckHpplay;
import com.sevtinge.hyperceiler.libhook.rules.milink.UnlockMiShare;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.milink.service")
public class MiLink extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new UnlockMiShare(), PrefsBridge.getBoolean("milink_unlock_mishare"));
        initHook(new AllowCameraDevices(), PrefsBridge.getBoolean("milink_allow_camera_devices"));
        initHook(new FuckHpplay(), PrefsBridge.getBoolean("milink_fuck_hpplay"));
    }
}
