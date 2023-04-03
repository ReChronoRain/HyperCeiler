package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.systemsettings.*;

public class SystemSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new VoipAssistantController(), mPrefsMap.getBoolean("system_settings_voip_assistant_controller"));
        initHook(new AddMiuiPlusEntry(), mPrefsMap.getBoolean("mirror_unlock_miui_plus"));
        initHook(new EnableSpeedMode(), mPrefsMap.getBoolean("system_settings_develop_speed_mode"));
        initHook(PermissionTopOfApp.INSTANCE, mPrefsMap.getBoolean("system_settings_permission_show_app_up"));
        initHook(new QuickInstallPermission(), mPrefsMap.getBoolean("system_settings_app_fast_install_permission"));
    }
}


