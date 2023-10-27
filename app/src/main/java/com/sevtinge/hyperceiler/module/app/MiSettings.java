package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.misettings.CustomRefreshRate;
import com.sevtinge.hyperceiler.module.hook.misettings.ShowMoreFpsList;

public class MiSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(CustomRefreshRate.INSTANCE, mPrefsMap.getBoolean("various_custom_refresh_rate"));
        initHook(ShowMoreFpsList.INSTANCE, mPrefsMap.getBoolean("mi_settings_show_fps"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
