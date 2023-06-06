package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.aiasst.AiCaptions;
import com.sevtinge.cemiuiler.module.base.BaseModule;

public class AiAsst extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new AiCaptions(), mPrefsMap.getBoolean("aiasst_ai_captions"));
    }
}
