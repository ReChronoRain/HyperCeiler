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
import com.sevtinge.hyperceiler.libhook.rules.misound.IncreaseSamplingRate;
import com.sevtinge.hyperceiler.libhook.rules.misound.NewAutoSEffSwitch;

@HookBase(targetPackage = "com.miui.misound")
public class MiSound extends BaseLoad {

    public MiSound() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        initHook(new NewAutoSEffSwitch(), mPrefsMap.getBoolean("misound_bluetooth"));
        initHook(IncreaseSamplingRate.INSTANCE, mPrefsMap.getBoolean("misound_increase_sampling_rate"));
    }
}
