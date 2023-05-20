package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.miinput.UnlockKnuckleFunction;
import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.systemsettings.*;
import com.sevtinge.cemiuiler.module.systemsettings.aiimage.*;
import com.sevtinge.cemiuiler.utils.SdkHelper;

public class SystemSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new ViewWifiPasswordHook(), mPrefsMap.getBoolean("system_settings_safe_wifi"));
        initHook(new VoipAssistantController(), mPrefsMap.getBoolean("system_settings_voip_assistant_controller"));
        initHook(new AddMiuiPlusEntry(), mPrefsMap.getBoolean("mirror_unlock_miui_plus"));
        initHook(new EnableSpeedMode(), mPrefsMap.getBoolean("system_settings_develop_speed_mode"));
        initHook(PermissionTopOfApp.INSTANCE, mPrefsMap.getBoolean("system_settings_permission_show_app_up"));
        initHook(new QuickInstallPermission(), mPrefsMap.getBoolean("system_settings_permission_unknown_origin_app"));
        initHook(new InternationalBuild(), mPrefsMap.getBoolean("system_settings_international_build"));

        initHook(new UnlockSuperResolution(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_sr"));
        initHook(new UnlockAi(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_ai"));
        initHook(new UnlockMemc(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_memc"));
        initHook(UnLockAreaScreenshot.INSTANCE, mPrefsMap.getBoolean("system_settings_area_screenshot"));

        if (SdkHelper.isAndroidR()) {
            initHook(new UnlockKnuckleFunction(), mPrefsMap.getBoolean("system_settings_knuckle_function"));
        }
    }
}


