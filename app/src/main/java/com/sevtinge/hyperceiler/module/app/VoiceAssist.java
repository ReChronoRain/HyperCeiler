package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.voiceassist.UseThirdPartyBrowser;

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

