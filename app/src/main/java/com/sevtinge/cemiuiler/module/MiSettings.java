package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.misettings.CustomRefreshRate;

public class MiSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new CustomRefreshRate(), mPrefsMap.getBoolean("various_custom_refresh_rate"));
    }
}
