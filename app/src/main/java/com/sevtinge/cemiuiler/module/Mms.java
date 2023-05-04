package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.mms.DisableAd;
import com.sevtinge.cemiuiler.module.mms.MmsDexKit;

public class Mms extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new MmsDexKit());
        initHook(new DisableAd(), mPrefsMap.getBoolean("mms_disable_ad"));
    }
}
