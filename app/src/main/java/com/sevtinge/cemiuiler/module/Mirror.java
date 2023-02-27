package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.mirror.UnlockMiuiPlus;

public class Mirror extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockMiuiPlus(), mPrefsMap.getBoolean("mirror_unlock_miui_plus"));
    }
}


