package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.mishare.NoAutoTurnOff;
import com.sevtinge.hyperceiler.module.hook.mishare.UnlockTurboMode;

public class MiShare extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(NoAutoTurnOff.INSTANCE, mPrefsMap.getBoolean("disable_mishare_auto_off"));
        initHook(UnlockTurboMode.INSTANCE, mPrefsMap.getBoolean("unlock_turbo_mode"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}


