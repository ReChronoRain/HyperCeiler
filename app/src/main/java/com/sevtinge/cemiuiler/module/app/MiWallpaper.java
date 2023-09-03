package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.miwallpaper.UnlockSuperWallpaper;

public class MiWallpaper extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockSuperWallpaper(), mPrefsMap.getBoolean("miwallpaper_unlock_super_wallpaper"));
    }
}



