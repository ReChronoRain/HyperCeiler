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
import com.sevtinge.hyperceiler.module.hook.phone.DisableRemoveNetworkMode;
import com.sevtinge.hyperceiler.module.hook.phone.DualNrSupport;
import com.sevtinge.hyperceiler.module.hook.phone.DualSaSupport;
import com.sevtinge.hyperceiler.module.hook.phone.ModemFeature;
import com.sevtinge.hyperceiler.module.hook.phone.N1BandPhone;
import com.sevtinge.hyperceiler.module.hook.phone.N28BandPhone;
import com.sevtinge.hyperceiler.module.hook.phone.N5N8BandPhone;
import com.sevtinge.hyperceiler.module.hook.phone.ViceSlotVolteButton;

@HookBase(targetPackage = "com.android.phone",  isPad = false)
public class Phone extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(ModemFeature.INSTANCE, mPrefsMap.getBoolean("phone_smart_dual_sim"));
        initHook(ViceSlotVolteButton.INSTANCE, mPrefsMap.getBoolean("phone_vice_slot_volte"));
        initHook(new DisableRemoveNetworkMode(), mPrefsMap.getBoolean("phone_disable_remove_network_mode"));

        initHook(DualNrSupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_nr"));
        initHook(DualSaSupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_sa"));
        initHook(N1BandPhone.INSTANCE, mPrefsMap.getBoolean("phone_n1"));
        initHook(N5N8BandPhone.INSTANCE, mPrefsMap.getBoolean("phone_n5_n8"));
        initHook(N28BandPhone.INSTANCE, mPrefsMap.getBoolean("phone_n28"));
    }
}
