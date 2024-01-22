package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.joyose.DisableCloudControl;
import com.sevtinge.hyperceiler.module.hook.joyose.EnableGpuTuner;

public class Joyose extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(DisableCloudControl.INSTANCE, mPrefsMap.getBoolean("various_disable_cloud_control"));
        initHook(EnableGpuTuner.INSTANCE, mPrefsMap.getBoolean("joyose_enable_gpu_tuner"));
    }
}
