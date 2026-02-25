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
import com.sevtinge.hyperceiler.libhook.rules.phone.DisableRemoveNetworkMode;
import com.sevtinge.hyperceiler.libhook.rules.phone.DualNrSupport;
import com.sevtinge.hyperceiler.libhook.rules.phone.DualSaSupport;
import com.sevtinge.hyperceiler.libhook.rules.phone.ModemFeature;
import com.sevtinge.hyperceiler.libhook.rules.phone.N1BandPhone;
import com.sevtinge.hyperceiler.libhook.rules.phone.N28BandPhone;
import com.sevtinge.hyperceiler.libhook.rules.phone.N5N8BandPhone;
import com.sevtinge.hyperceiler.libhook.rules.phone.UnlockVoiceLink;
import com.sevtinge.hyperceiler.libhook.rules.phone.ViceSlotVolteButton;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.android.phone")
public class Phone extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        initHook(new UnlockVoiceLink(), PrefsBridge.getBoolean("phone_unlock_voice_link"));
        initHook(ModemFeature.INSTANCE, PrefsBridge.getBoolean("phone_smart_dual_sim"));
        initHook(ViceSlotVolteButton.INSTANCE, PrefsBridge.getBoolean("phone_vice_slot_volte"));
        initHook(new DisableRemoveNetworkMode(), PrefsBridge.getBoolean("phone_disable_remove_network_mode"));

        initHook(DualNrSupport.INSTANCE, PrefsBridge.getBoolean("phone_double_5g_nr"));
        initHook(DualSaSupport.INSTANCE, PrefsBridge.getBoolean("phone_double_5g_sa"));
        initHook(N1BandPhone.INSTANCE, PrefsBridge.getBoolean("phone_n1"));
        initHook(N5N8BandPhone.INSTANCE, PrefsBridge.getBoolean("phone_n5_n8"));
        initHook(N28BandPhone.INSTANCE, PrefsBridge.getBoolean("phone_n28"));
    }
}
