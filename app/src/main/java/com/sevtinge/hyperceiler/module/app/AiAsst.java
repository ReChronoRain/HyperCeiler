package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.aiasst.AiCaptions;
import com.sevtinge.hyperceiler.module.hook.aiasst.DisableWatermark;


public class AiAsst extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);

        initHook(new AiCaptions(), mPrefsMap.getBoolean("aiasst_ai_captions"));
        initHook(new DisableWatermark(), mPrefsMap.getBoolean("aiasst_disable_watermark"));

        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
