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
import com.sevtinge.hyperceiler.module.hook.misound.BluetoothListener;
import com.sevtinge.hyperceiler.module.hook.misound.IncreaseSamplingRate;

@HookBase(targetPackage = "com.miui.misound",  isPad = false)
public class MiSound extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new BluetoothListener(), mPrefsMap.getBoolean("misound_bluetooth"));
        initHook(IncreaseSamplingRate.INSTANCE, mPrefsMap.getBoolean("misound_increase_sampling_rate"));
    }
}
