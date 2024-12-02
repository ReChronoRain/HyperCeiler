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
import com.sevtinge.hyperceiler.module.hook.milink.AllowCameraDevices;
import com.sevtinge.hyperceiler.module.hook.milink.FuckHpplay;
import com.sevtinge.hyperceiler.module.hook.milink.NewUnlockHMind;
import com.sevtinge.hyperceiler.module.hook.milink.UnlockMiShare;

@HookBase(targetPackage = "com.milink.service",  isPad = false)
public class MiLink extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockMiShare(), mPrefsMap.getBoolean("milink_unlock_mishare"));
        initHook(NewUnlockHMind.INSTANCE, mPrefsMap.getBoolean("milink_unlock_hmind"));
        initHook(new AllowCameraDevices(), mPrefsMap.getBoolean("milink_allow_camera_devices"));
        initHook(new FuckHpplay(), mPrefsMap.getBoolean("milink_fuck_hpplay"));
    }
}
