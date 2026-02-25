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
import com.sevtinge.hyperceiler.libhook.rules.weather.SetCardLightDarkMode;
import com.sevtinge.hyperceiler.libhook.rules.weather.SetDeviceLevel;
import com.sevtinge.hyperceiler.libhook.rules.weather.UnlockSuperBlur;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.miui.weather2")
public class Weather extends BaseLoad {

    public Weather() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        initHook(new SetCardLightDarkMode(), PrefsBridge.getStringAsInt("weather_card_display_type", 0) != 0);
        initHook(new SetDeviceLevel(), PrefsBridge.getStringAsInt("weather_device_level", 3) != 3);
        initHook(UnlockSuperBlur.INSTANCE, PrefsBridge.getBoolean("weather_unlock_blur"));
    }
}
