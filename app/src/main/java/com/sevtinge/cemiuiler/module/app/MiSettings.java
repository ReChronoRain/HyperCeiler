package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.misettings.CustomRefreshRate;
import com.sevtinge.cemiuiler.module.hook.misettings.MiSettingsDexKit;
import com.sevtinge.cemiuiler.module.hook.misettings.ShowMoreFpsList;

public class MiSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new MiSettingsDexKit());
        initHook(CustomRefreshRate.INSTANCE, mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(ShowMoreFpsList.INSTANCE, mPrefsMap.getBoolean("mi_settings_show_fps"));
    }
}
