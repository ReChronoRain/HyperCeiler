package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.aiasst.AiCaptions;


public class AiAsst extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new AiCaptions(), mPrefsMap.getBoolean("aiasst_ai_captions"));
    }
}
