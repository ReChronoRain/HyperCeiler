package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.aireco.DeviceModify;

public class Aireco extends BaseModule {

        @Override
        public void handleLoadPackage() {
            initHook(new DeviceModify(), mPrefsMap.getBoolean("aireco_device_modify"));
        }
    }




