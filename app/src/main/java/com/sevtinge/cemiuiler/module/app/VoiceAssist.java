package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.voiceassist.UseThirdPartyBrowser;
import com.sevtinge.cemiuiler.module.hook.voiceassist.VoiceAssistDexKit;

public class VoiceAssist extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new VoiceAssistDexKit());
        initHook(new UseThirdPartyBrowser(), mPrefsMap.getBoolean("content_extension_browser"));
    }
}

