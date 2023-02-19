package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.browser.DebugMode;

public class Browser extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DebugMode(), mPrefsMap.getBoolean("browser_debug_mode"));
    }
}
