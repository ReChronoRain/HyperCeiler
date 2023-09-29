package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.networkboost.LinkTurboToast;

public class NetworkBoost extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new LinkTurboToast(), mPrefsMap.getBoolean("various_disable_link_turbo_toast"));
    }
}
