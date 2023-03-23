package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.systemsettings.AddMiuiPlusEntry;
import com.sevtinge.cemiuiler.module.systemsettings.EnableSpeedMode;
import com.sevtinge.cemiuiler.module.systemsettings.PermissionTopOfApp;
import com.sevtinge.cemiuiler.module.systemsettings.VoipAssistantController;

public class SystemSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new VoipAssistantController(), mPrefsMap.getBoolean("system_settings_voip_assistant_controller"));
        initHook(new AddMiuiPlusEntry(), mPrefsMap.getBoolean("mirror_unlock_miui_plus"));
        initHook(new EnableSpeedMode(), mPrefsMap.getBoolean("system_settings_develop_speed_mode"));
        initHook(PermissionTopOfApp.INSTANCE, mPrefsMap.getBoolean("system_settings_permission_show_app_up"));
    }
}


