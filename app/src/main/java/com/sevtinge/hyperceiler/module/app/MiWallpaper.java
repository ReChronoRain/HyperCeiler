package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.miwallpaper.UnlockSuperWallpaper;

public class MiWallpaper extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockSuperWallpaper(), mPrefsMap.getBoolean("miwallpaper_unlock_super_wallpaper"));
    }
}



