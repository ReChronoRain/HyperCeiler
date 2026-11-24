package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.voiceassist.OpenRecognizedLinksInDefaultBrowser;
import com.sevtinge.hyperceiler.libhook.rules.voiceassist.RemoveScreenTransWatermark;

@HookBase(targetPackage = "com.miui.voiceassist")
public class VoiceAssist extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        initHook(OpenRecognizedLinksInDefaultBrowser.INSTANCE, PrefsBridge.getBoolean("voiceassist_use_default_browser"));
        initHook(new RemoveScreenTransWatermark(), PrefsBridge.getBoolean("voiceassist_remove_screen_trans_watermark"));
    }
}
