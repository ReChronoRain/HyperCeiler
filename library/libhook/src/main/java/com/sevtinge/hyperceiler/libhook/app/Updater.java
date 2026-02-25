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

import android.text.TextUtils;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.updater.AndroidVersionCode;
import com.sevtinge.hyperceiler.libhook.rules.updater.AutoUpdateDialog;
import com.sevtinge.hyperceiler.libhook.rules.updater.DeviceModify;
import com.sevtinge.hyperceiler.libhook.rules.updater.VabUpdate;
import com.sevtinge.hyperceiler.libhook.rules.updater.VersionCodeModify;
import com.sevtinge.hyperceiler.libhook.rules.updater.VersionCodeNew;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.android.updater")
public class Updater extends BaseLoad {

    public Updater() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        if (PrefsBridge.getBoolean("updater_enable_miui_version")) {
            if (PrefsBridge.getStringAsInt("updater_version_mode", 1) != 1) {
                initHook(VersionCodeNew.INSTANCE, true);
            } else {
                initHook(new VersionCodeModify(), !TextUtils.isEmpty(PrefsBridge.getString("various_updater_miui_version", "")));
            }
            initHook(AndroidVersionCode.INSTANCE, !TextUtils.isEmpty(PrefsBridge.getString("various_updater_android_version", "")));
            initHook(DeviceModify.INSTANCE, !TextUtils.isEmpty(PrefsBridge.getString("updater_device", "")));
        }
        initHook(new VabUpdate(), PrefsBridge.getBoolean("updater_fuck_vab"));
        initHook(AutoUpdateDialog.INSTANCE, PrefsBridge.getBoolean("updater_diable_dialog"));
    }
}
