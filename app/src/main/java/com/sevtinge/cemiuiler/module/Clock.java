package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.clock.EnableHourGlass;

public class Clock extends BaseModule {

        @Override
        public void handleLoadPackage() {
            initHook(new EnableHourGlass(), mPrefsMap.getBoolean("clock_enable_hour_glass"));
        }
    }


