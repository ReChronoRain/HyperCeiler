package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.music.DisableAd;

public class Music extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableAd(), mPrefsMap.getBoolean("music_disable_ad"));
    }
}


