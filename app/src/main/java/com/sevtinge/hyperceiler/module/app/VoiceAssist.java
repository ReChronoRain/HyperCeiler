package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.voiceassist.DisableChatWatermark;
import com.sevtinge.hyperceiler.module.hook.voiceassist.UseThirdPartyBrowser;

public class VoiceAssist extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(UseThirdPartyBrowser.INSTANCE, mPrefsMap.getBoolean("content_extension_browser"));
        initHook(new DisableChatWatermark(), mPrefsMap.getBoolean("voiceassist_disable_watermark"));
    }
}

