package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.mms.DisableAd;
import com.sevtinge.cemiuiler.module.hook.mms.MmsDexKit;

public class Mms extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new MmsDexKit());
        initHook(new DisableAd(), mPrefsMap.getBoolean("mms_disable_ad"));
    }
}
