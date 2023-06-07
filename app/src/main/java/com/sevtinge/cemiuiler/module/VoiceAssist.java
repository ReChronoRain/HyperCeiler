package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.voiceassist.UseThirdPartyBrowser;
import com.sevtinge.cemiuiler.module.voiceassist.VoiceAssistDexKit;

public class VoiceAssist extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new VoiceAssistDexKit());
        initHook(new UseThirdPartyBrowser(), mPrefsMap.getBoolean("content_extension_browser"));
    }
}

