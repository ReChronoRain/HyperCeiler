/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.app.SystemSettings.Phone;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.systemsettings.AddGoogleListHeader;
import com.sevtinge.hyperceiler.module.hook.systemsettings.AddMiuiPlusEntry;
import com.sevtinge.hyperceiler.module.hook.systemsettings.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.module.hook.systemsettings.AntiQues;
import com.sevtinge.hyperceiler.module.hook.systemsettings.AppsFreezerEnable;
import com.sevtinge.hyperceiler.module.hook.systemsettings.DisableInstallUnknownVerify;
import com.sevtinge.hyperceiler.module.hook.systemsettings.EnableFoldArea;
import com.sevtinge.hyperceiler.module.hook.systemsettings.EnablePadArea;
import com.sevtinge.hyperceiler.module.hook.systemsettings.EnableSpeedMode;
import com.sevtinge.hyperceiler.module.hook.systemsettings.HyperCeilerSettings;
import com.sevtinge.hyperceiler.module.hook.systemsettings.InternationalBuild;
import com.sevtinge.hyperceiler.module.hook.systemsettings.LanguageMenuShowAllApps;
import com.sevtinge.hyperceiler.module.hook.systemsettings.LinkTurbo;
import com.sevtinge.hyperceiler.module.hook.systemsettings.ModifySystemVersion;
import com.sevtinge.hyperceiler.module.hook.systemsettings.MoreNotificationSettings;
import com.sevtinge.hyperceiler.module.hook.systemsettings.MoreVpnTypes;
import com.sevtinge.hyperceiler.module.hook.systemsettings.NewNFCPage;
import com.sevtinge.hyperceiler.module.hook.systemsettings.NoveltyHaptic;
import com.sevtinge.hyperceiler.module.hook.systemsettings.QuickManageOverlayPermission;
import com.sevtinge.hyperceiler.module.hook.systemsettings.QuickManageUnknownAppSources;
import com.sevtinge.hyperceiler.module.hook.systemsettings.QuickManagerAccessibilityPermission;
import com.sevtinge.hyperceiler.module.hook.systemsettings.RunningServices;
import com.sevtinge.hyperceiler.module.hook.systemsettings.ShowAutoUIMode;
import com.sevtinge.hyperceiler.module.hook.systemsettings.UnLockAreaScreenshot;
import com.sevtinge.hyperceiler.module.hook.systemsettings.UnlockMaxFps;
import com.sevtinge.hyperceiler.module.hook.systemsettings.UnlockNeverSleepScreen;
import com.sevtinge.hyperceiler.module.hook.systemsettings.UnlockTaplusForSettings;
import com.sevtinge.hyperceiler.module.hook.systemsettings.UsbModeChoose;
import com.sevtinge.hyperceiler.module.hook.systemsettings.ViewWifiPasswordHook;
import com.sevtinge.hyperceiler.module.hook.systemsettings.VoipAssistantController;
import com.sevtinge.hyperceiler.module.hook.systemsettings.VolumeSeparateControlForSettings;
import com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage.UnlockAi;
import com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage.UnlockMemc;
import com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage.UnlockSuperResolution;

@HookBase(targetPackage = "com.android.settings", isPad = false, targetSdk = 34)
public class SystemSettingsU extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new HyperCeilerSettings(), mPrefsMap.getStringAsInt("settings_icon", 0) != 0);

        initHook(new ShowAutoUIMode(), mPrefsMap.getBoolean("system_settings_unlock_ui_mode"));
        initHook(new LinkTurbo(), mPrefsMap.getBoolean("system_settings_linkturbo"));
        initHook(new RunningServices(), true); // 显示原生内存信息
        initHook(new UsbModeChoose(), mPrefsMap.getStringAsInt("system_settings_usb_mode_choose", 0) != 0
                || mPrefsMap.getBoolean("system_settings_usb_mode"));
        initHook(new ViewWifiPasswordHook(), mPrefsMap.getBoolean("system_settings_safe_wifi"));
        initHook(new VoipAssistantController(), mPrefsMap.getBoolean("system_settings_voip_assistant_controller"));
        initHook(new MoreVpnTypes(), mPrefsMap.getBoolean("system_settings_more_vpn_types"));
        initHook(new AddMiuiPlusEntry(), mPrefsMap.getBoolean("mirror_unlock_miui_plus"));
        initHook(new EnableSpeedMode(), mPrefsMap.getBoolean("system_settings_develop_speed_mode"));
        initHook(new QuickManageOverlayPermission(), mPrefsMap.getBoolean("system_settings_permission_show_app_up"));
        initHook(new QuickManageUnknownAppSources(), mPrefsMap.getBoolean("system_settings_permission_unknown_origin_app"));
        initHook(new QuickManagerAccessibilityPermission(), mPrefsMap.getBoolean("system_settings_permission_accessibility"));
        initHook(new InternationalBuild(), mPrefsMap.getBoolean("system_settings_international_build"));
        initHook(new DisableInstallUnknownVerify(), mPrefsMap.getBoolean("system_settings_permission_disable_install_unknown_verify"));
        initHook(new NewNFCPage(), mPrefsMap.getBoolean("system_settings_new_nfc_page"));
        initHook(new AppsFreezerEnable(), mPrefsMap.getBoolean("system_settings_apps_freezer"));
        // initHook(new BluetoothRestrict(), mPrefsMap.getBoolean("various_disable_bluetooth_restrict"));
        initHook(new VolumeSeparateControlForSettings(), mPrefsMap.getBoolean("system_framework_volume_separate_control") && !isMoreHyperOSVersion(1f));
        initHook(UnlockMaxFps.INSTANCE, mPrefsMap.getBoolean("system_settings_develop_max_fps"));
        initHook(new AntiQues(), mPrefsMap.getBoolean("system_settings_anti_ques"));

        initHook(new UnlockSuperResolution(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_sr"));
        initHook(new UnlockAi(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_ai"));
        initHook(new UnlockMemc(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_memc"));
        initHook(UnLockAreaScreenshot.INSTANCE, mPrefsMap.getBoolean("system_settings_area_screenshot"));
        initHook(NoveltyHaptic.INSTANCE, mPrefsMap.getBoolean("system_settings_novelty_haptic"));
        initHook(new MoreNotificationSettings(), mPrefsMap.getBoolean("system_settings_more_notification_settings"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));
        initHook(new LanguageMenuShowAllApps(), mPrefsMap.getBoolean("system_settings_lang_menu_shouw_all_app"));

        initHook(new EnablePadArea(), mPrefsMap.getBoolean("system_settings_enable_pad_area"));
        initHook(new EnableFoldArea(), mPrefsMap.getBoolean("system_settings_enable_fold_area"));

        initHook(new ModifySystemVersion(), mPrefsMap.getBoolean("updater_enable_miui_version") && mPrefsMap.getStringAsInt("updater_version_mode", 1) != 1);

        initHook(UnlockTaplusForSettings.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));

        initHook(new AddGoogleListHeader(), mPrefsMap.getBoolean("system_settings_unlock_google_header"));

        initHook(new UnlockNeverSleepScreen(), mPrefsMap.getBoolean("system_settings_allow_never_lock_screen"));

    }
}
