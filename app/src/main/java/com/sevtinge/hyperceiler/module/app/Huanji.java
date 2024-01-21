package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.huanji.AllowMoveAllApps;

public class Huanji extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new AllowMoveAllApps(), mPrefsMap.getBoolean("huanji_allow_all_apps"));
    }
}
