package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.milink.UnlockHMind;
import com.sevtinge.hyperceiler.module.hook.milink.UnlockMiShare;

public class MiLink extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(LoadHostDir.INSTANCE);
        initHook(new UnlockMiShare(), mPrefsMap.getBoolean("milink_unlock_mishare"));
        initHook(new UnlockHMind(), mPrefsMap.getBoolean("milink_unlock_hmind"));
        initHook(CloseHostDir.INSTANCE);
    }
}

