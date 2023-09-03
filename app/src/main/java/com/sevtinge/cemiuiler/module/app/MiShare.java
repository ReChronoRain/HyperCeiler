package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.mishare.MiShareDexKit;
import com.sevtinge.cemiuiler.module.hook.mishare.NoAutoTurnOff;

public class MiShare extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new MiShareDexKit());
        initHook(NoAutoTurnOff.INSTANCE, mPrefsMap.getBoolean("disable_mishare_auto_off"));
    }
}


