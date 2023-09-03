package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.hook.aireco.DeviceModify;
import com.sevtinge.cemiuiler.module.base.BaseModule;

public class Aireco extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DeviceModify(), mPrefsMap.getBoolean("aireco_device_modify"));
    }
}




