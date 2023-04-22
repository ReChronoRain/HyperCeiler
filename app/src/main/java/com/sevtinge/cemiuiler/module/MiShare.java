package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.mishare.DisableMishareAutoOff;
import com.sevtinge.cemiuiler.module.mishare.NoAutoTurnOff;

public class MiShare extends BaseModule {

    @Override
    public void handleLoadPackage() {
        //initHook(new DisableMishareAutoOff(), mPrefsMap.getBoolean("disable_mishare_auto_off"));
        initHook(new NoAutoTurnOff(), mPrefsMap.getBoolean("disable_mishare_auto_off"));
    }
}


