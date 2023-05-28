package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.misettings.CustomRefreshRate;
import com.sevtinge.cemiuiler.module.misettings.ShowMoreFpsList;

public class MiSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new CustomRefreshRate(), mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(ShowMoreFpsList.INSTANCE, mPrefsMap.getBoolean("mi_settings_show_fps"));
    }
}
