package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.systemsettings.AddMiuiPlusEntry;
import com.sevtinge.hyperceiler.module.hook.systemsettings.AppsFreezerEnable;
import com.sevtinge.hyperceiler.module.hook.systemsettings.EnableFoldArea;
import com.sevtinge.hyperceiler.module.hook.systemsettings.EnablePadArea;
import com.sevtinge.hyperceiler.module.hook.systemsettings.EnableSpeedMode;
import com.sevtinge.hyperceiler.module.hook.systemsettings.InternationalBuild;
import com.sevtinge.hyperceiler.module.hook.systemsettings.NewNFCPage;
import com.sevtinge.hyperceiler.module.hook.systemsettings.NoveltyHaptic;
import com.sevtinge.hyperceiler.module.hook.systemsettings.QuickManageOverlayPermission;
import com.sevtinge.hyperceiler.module.hook.systemsettings.QuickManageUnknownAppSources;
import com.sevtinge.hyperceiler.module.hook.systemsettings.UnLockAreaScreenshot;
import com.sevtinge.hyperceiler.module.hook.systemsettings.UnlockTaplusForSettings;
import com.sevtinge.hyperceiler.module.hook.systemsettings.ViewWifiPasswordHook;
import com.sevtinge.hyperceiler.module.hook.systemsettings.VoipAssistantController;
import com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage.UnlockAi;
import com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage.UnlockMemc;
import com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage.UnlockSuperResolution;

public class SystemSettings extends BaseModule {

    @Override
    public void handleLoadPackage() {
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

        initHook(new EnablePadArea(),mPrefsMap.getBoolean("system_settings_enable_pad_area"));
        initHook(new EnableFoldArea(),mPrefsMap.getBoolean("system_settings_enable_fold_area"));

        if (!isAndroidR()) {
            initHook(UnlockTaplusForSettings.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));
        }
    }
}


