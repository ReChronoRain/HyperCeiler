package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.settings.HyperCeilerSettings;
import com.sevtinge.hyperceiler.module.hook.settings.VolumeSeparateControlForSettings;

public class Settings extends BaseModule {

    @Override
    public void handleLoadPackage() {

        // initHook(new BluetoothRestrict(), mPrefsMap.getBoolean("various_disable_bluetooth_restrict"));
        initHook(new VolumeSeparateControlForSettings(), mPrefsMap.getBoolean("system_framework_volume_separate_control"));


        initHook(new HyperCeilerSettings(), mPrefsMap.getStringAsInt("settings_icon", 0) != 0);
    }

}
