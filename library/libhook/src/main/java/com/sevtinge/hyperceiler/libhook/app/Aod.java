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

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.aod.UnlockAiWallpaper;
import com.sevtinge.hyperceiler.libhook.rules.aod.UnlockAodAon;
import com.sevtinge.hyperceiler.libhook.rules.aod.UnlockShortCuts;

@HookBase(targetPackage = "com.miui.aod")
public class Aod extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(UnlockShortCuts.INSTANCE, isMoreSmallVersion(200, 2f));
        // initHook(new UnlockAlwaysOnDisplay(), mPrefsMap.getBoolean("aod_unlock_always_on_display_hyper"));
        initHook(new UnlockAodAon(), mPrefsMap.getBoolean("aod_unlock_aon"));
        initHook(UnlockAiWallpaper.INSTANCE, mPrefsMap.getBoolean("aod_unlock_ai_wallpaper"));
    }
}
