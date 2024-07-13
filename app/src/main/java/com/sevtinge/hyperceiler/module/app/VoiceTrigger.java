package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.HookExpand;
import com.sevtinge.hyperceiler.module.hook.voicetrigger.BypassUDKWordLegalCheck;

@HookExpand(pkg = "com.miui.voicetrigger", isPad = false, tarAndroid = 33)
public class VoiceTrigger extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(BypassUDKWordLegalCheck.INSTANCE, mPrefsMap.getBoolean("bypass_voicetrigger_udk_legalcheck"));
    }
}
