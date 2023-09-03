package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.phone.DualNrSupport;
import com.sevtinge.cemiuiler.module.hook.phone.DualSaSupport;
import com.sevtinge.cemiuiler.module.hook.phone.ModemFeature;
import com.sevtinge.cemiuiler.module.hook.phone.N1BandPhone;
import com.sevtinge.cemiuiler.module.hook.phone.N28BandPhone;
import com.sevtinge.cemiuiler.module.hook.phone.N5N8BandPhone;
import com.sevtinge.cemiuiler.module.hook.phone.ViceSlotVolteButton;

public class Phone extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(ModemFeature.INSTANCE, mPrefsMap.getBoolean("phone_smart_dual_sim"));
        initHook(ViceSlotVolteButton.INSTANCE, mPrefsMap.getBoolean("phone_vice_slot_volte"));

        initHook(DualNrSupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_nr"));
        initHook(DualSaSupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_sa"));
        initHook(N1BandPhone.INSTANCE, mPrefsMap.getBoolean("phone_n1"));
        initHook(N5N8BandPhone.INSTANCE, mPrefsMap.getBoolean("phone_n5_n8"));
        initHook(N28BandPhone.INSTANCE, mPrefsMap.getBoolean("phone_n28"));
    }
}
