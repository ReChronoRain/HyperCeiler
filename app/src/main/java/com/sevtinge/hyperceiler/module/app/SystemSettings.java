package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.systemsettings.*;
import com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage.*;

public class SystemSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new LinkTurbo(), mPrefsMap.getBoolean("system_settings_linkturbo"));
        initHook(new ViewWifiPasswordHook(), mPrefsMap.getBoolean("system_settings_safe_wifi"));
        initHook(new VoipAssistantController(), mPrefsMap.getBoolean("system_settings_voip_assistant_controller"));
        initHook(new AddMiuiPlusEntry(), mPrefsMap.getBoolean("mirror_unlock_miui_plus"));
        initHook(new EnableSpeedMode(), mPrefsMap.getBoolean("system_settings_develop_speed_mode"));
        initHook(new QuickManageOverlayPermission(), mPrefsMap.getBoolean("system_settings_permission_show_app_up"));
        initHook(new QuickManageUnknownAppSources(), mPrefsMap.getBoolean("system_settings_permission_unknown_origin_app"));
        initHook(new InternationalBuild(), mPrefsMap.getBoolean("system_settings_international_build"));
        initHook(new NewNFCPage(), mPrefsMap.getBoolean("system_settings_new_nfc_page"));
        initHook(new AppsFreezerEnable(), mPrefsMap.getBoolean("system_settings_apps_freezer"));

        initHook(new UnlockSuperResolution(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_sr"));
        initHook(new UnlockAi(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_ai"));
        initHook(new UnlockMemc(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_memc"));
        initHook(UnLockAreaScreenshot.INSTANCE, mPrefsMap.getBoolean("system_settings_area_screenshot"));
        initHook(NoveltyHaptic.INSTANCE, mPrefsMap.getBoolean("system_settings_novelty_haptic"));
        initHook(new MoreNotificationSettings(), mPrefsMap.getBoolean("system_settings_more_notification_settings"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));

        initHook(new EnablePadArea(), mPrefsMap.getBoolean("system_settings_enable_pad_area"));
        initHook(new EnableFoldArea(), mPrefsMap.getBoolean("system_settings_enable_fold_area"));

        initHook(new ModifySystemVersion(), mPrefsMap.getBoolean("updater_enable_miui_version") && mPrefsMap.getStringAsInt("updater_version_mode", 1) != 1);

        if (!isAndroidVersion(30)) {
            initHook(UnlockTaplusForSettings.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));
        }

        initHook(new UnlockBackgroundBlur(), mPrefsMap.getBoolean("system_settings_display_unlock_background_blur"));
    }
}


