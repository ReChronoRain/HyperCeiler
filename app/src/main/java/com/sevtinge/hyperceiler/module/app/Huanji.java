package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.huanji.AllowMoveAllApps;

public class Huanji extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(new AllowMoveAllApps(), mPrefsMap.getBoolean("huanji_allow_all_apps"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
