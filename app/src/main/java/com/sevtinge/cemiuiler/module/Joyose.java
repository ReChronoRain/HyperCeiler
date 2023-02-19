package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.joyose.DisableCloudControl;

public class Joyose extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableCloudControl(), mPrefsMap.getBoolean("various_disable_cloud_control"));
    }
}
