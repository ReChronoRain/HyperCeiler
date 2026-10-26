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
package com.sevtinge.hyperceiler.libhook.app.Home.os4;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.home.HomePortraitReverse;
import com.sevtinge.hyperceiler.libhook.rules.home.gesture.HomeGestureOS4;
import com.sevtinge.hyperceiler.libhook.rules.home.other.HomeSettingsOS4;
import com.sevtinge.hyperceiler.libhook.rules.home.other.NativeHomeHooksOS4;

@HookBase(targetPackage = "com.miui.home", deviceType = 2, minOSVersion = 4.0F)
public class HomePhone extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        NativeHomeHooksOS4.INSTANCE.init();
        initHook(HomeSettingsOS4.INSTANCE, true);
        // OS4 dock background is installed by SystemFrameworkB (HomeDockWindow).
        initHook(HomeGestureOS4.INSTANCE, true);
        initHook(new HomePortraitReverse(), PrefsBridge.getBoolean("home_other_portrait_reverse"));
    }
}
