package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.voiceassist.*;

public class VoiceAssist extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(UseThirdPartyBrowser.INSTANCE, mPrefsMap.getBoolean("content_extension_browser"));
        initHook(new DisableChatWatermark(), mPrefsMap.getBoolean("voiceassist_disable_watermark"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}

