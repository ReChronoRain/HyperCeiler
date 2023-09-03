package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.creation.UnlockCreation;

public class Creation extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(UnlockCreation.INSTANCE, mPrefsMap.getBoolean("creation_unlock_enable"));
    }
}
