package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.base.CloseHostDir;
import com.sevtinge.cemiuiler.module.base.LoadHostDir;
import com.sevtinge.cemiuiler.module.hook.voiceassist.UseThirdPartyBrowser;

public class VoiceAssist extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(UseThirdPartyBrowser.INSTANCE, mPrefsMap.getBoolean("content_extension_browser"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}

