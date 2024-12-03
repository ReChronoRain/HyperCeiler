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
import com.sevtinge.hyperceiler.module.hook.misettings.CustomRefreshRate;
import com.sevtinge.hyperceiler.module.hook.misettings.ShowMoreFpsList;

@HookBase(targetPackage = "com.xiaomi.misettings",  isPad = false)
public class MiSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(CustomRefreshRate.INSTANCE, mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(ShowMoreFpsList.INSTANCE, mPrefsMap.getBoolean("mi_settings_show_fps"));
    }
}
