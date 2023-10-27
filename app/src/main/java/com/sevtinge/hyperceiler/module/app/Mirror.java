package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.mirror.UnlockMiuiPlus;

public class Mirror extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockMiuiPlus(), mPrefsMap.getBoolean("mirror_unlock_miui_plus"));
    }
}


