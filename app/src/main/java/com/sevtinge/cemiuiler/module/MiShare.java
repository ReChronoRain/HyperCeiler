package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.mishare.DisableMishareAutoOff;

public class MiShare extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableMishareAutoOff(), mPrefsMap.getBoolean("disable_mishare_auto_off"));
    }
}


