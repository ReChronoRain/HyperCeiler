package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.music.DisableAd;

public class Music extends BaseModule {

        @Override
        public void handleLoadPackage() {
            initHook(new DisableAd(), mPrefsMap.getBoolean("music_disable_ad"));
        }
    }


