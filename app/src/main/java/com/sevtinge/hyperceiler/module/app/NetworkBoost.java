package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.networkboost.LinkTurboToast;

public class NetworkBoost extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new LinkTurboToast(), mPrefsMap.getBoolean("various_disable_link_turbo_toast"));
    }
}
