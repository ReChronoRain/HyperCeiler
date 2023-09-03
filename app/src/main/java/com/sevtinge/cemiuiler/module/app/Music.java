package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.music.DisableAd;

public class Music extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableAd(), mPrefsMap.getBoolean("music_disable_ad"));
    }
}


