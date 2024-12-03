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
import com.sevtinge.hyperceiler.module.hook.joyose.DisableCloudControl;
import com.sevtinge.hyperceiler.module.hook.joyose.EnableGpuTuner;

@HookBase(targetPackage = "com.xiaomi.joyose",  isPad = false)
public class Joyose extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableCloudControl(), mPrefsMap.getBoolean("various_disable_cloud_control"));
        initHook(new EnableGpuTuner(), mPrefsMap.getBoolean("joyose_enable_gpu_tuner"));
    }
}
