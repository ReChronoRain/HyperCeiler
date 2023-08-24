package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.creation.UnlockCreation;

public class Creation extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(UnlockCreation.INSTANCE, mPrefsMap.getBoolean("creation_unlock_enable"));
    }
}
