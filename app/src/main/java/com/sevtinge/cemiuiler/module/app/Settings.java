package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.settings.CemiuilerSettings;
import com.sevtinge.cemiuiler.module.hook.settings.NotificationImportance;
import com.sevtinge.cemiuiler.module.hook.settings.VolumeSeparateControlForSettings;

public class Settings extends BaseModule {

    @Override
    public void handleLoadPackage() {

        initHook(new NotificationImportance(), mPrefsMap.getBoolean("settings_notfication_importance"));

        // initHook(new BluetoothRestrict(), mPrefsMap.getBoolean("various_disable_bluetooth_restrict"));
        initHook(new VolumeSeparateControlForSettings(), mPrefsMap.getBoolean("system_framework_volume_separate_control"));


        initHook(new CemiuilerSettings(), mPrefsMap.getStringAsInt("settings_icon", 0) != 0);
    }

}
