package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.mms.DisableAd;

public class Mms extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new DisableAd(), mPrefsMap.getBoolean("mms_disable_ad"));
    }
}
