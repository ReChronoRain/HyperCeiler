package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.base.CloseHostDir;
import com.sevtinge.cemiuiler.module.base.LoadHostDir;
import com.sevtinge.cemiuiler.module.hook.joyose.DisableCloudControl;
import com.sevtinge.cemiuiler.module.hook.joyose.EnableGpuTuner;

public class Joyose extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(DisableCloudControl.INSTANCE, mPrefsMap.getBoolean("various_disable_cloud_control"));
        initHook(EnableGpuTuner.INSTANCE, mPrefsMap.getBoolean("joyose_enable_gpu_tuner"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
