package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.systemui.BluetoothRestrict;
import com.sevtinge.cemiuiler.module.settings.CemiuilerSettings;
import com.sevtinge.cemiuiler.module.settings.NotificationImportance;
import com.sevtinge.cemiuiler.module.settings.VolumeSeparateControlForSettings;

public class Settings extends BaseModule {

    @Override
    public void handleLoadPackage() {

        initHook(new NotificationImportance(), mPrefsMap.getBoolean("settings_notfication_importance"));

        //initHook(new BluetoothRestrict(), mPrefsMap.getBoolean("various_disable_bluetooth_restrict"));
        initHook(new VolumeSeparateControlForSettings(), mPrefsMap.getBoolean("system_framework_volume_separate_control"));


        initHook(new CemiuilerSettings(), mPrefsMap.getStringAsInt("settings_icon", 0) != 0);
    }

    /*public static void handleLoad(LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;

        initHook(new NotificationVolumeSettingsHook(), mPrefsMap.getBoolean("system_framework_separate_volume"));
    }*/
}
