package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.clock.EnableHourGlass;

public class Clock extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new EnableHourGlass(), mPrefsMap.getBoolean("clock_enable_hour_glass"));
    }
}


