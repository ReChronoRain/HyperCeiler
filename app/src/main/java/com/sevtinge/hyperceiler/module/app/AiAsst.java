package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.aiasst.AiCaptions;


public class AiAsst extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new AiCaptions(), mPrefsMap.getBoolean("aiasst_ai_captions"));
    }
}
