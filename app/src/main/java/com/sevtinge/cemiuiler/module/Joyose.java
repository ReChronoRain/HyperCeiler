package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.joyose.DisableCloudControl;
import com.sevtinge.cemiuiler.module.joyose.EnableGpuTuner;
import com.sevtinge.cemiuiler.module.joyose.JoyoseDexKit;

public class Joyose extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new JoyoseDexKit());
        initHook(new DisableCloudControl(), mPrefsMap.getBoolean("various_disable_cloud_control"));
        initHook(new EnableGpuTuner(), mPrefsMap.getBoolean("joyose_enable_gpu_tuner"));
    }
}
