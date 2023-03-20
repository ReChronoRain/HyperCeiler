package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.systemuiplugin.EnableVolumeBlur;

public class SystemUIPlugin extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new EnableVolumeBlur(), mPrefsMap.getBoolean("system_ui_plugin_enable_volume_blur"));
    }
}
