/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.app.SystemFramework.Phone;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.AllowDisableProtectedPackage;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.AntiQues;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.AppLinkVerify;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.AutoEffectSwitchForSystem;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.BackgroundBlur;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.BypassForceDownloadui;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.BypassForceMiAppStore;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.BypassUnknownSourcesRestrictions;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.CleanOpenMenu;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.CleanProcessTextMenu;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.CleanShareMenu;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.ClipboardWhitelist;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.ConservativeMilletFramework;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DeleteOnPostNotification;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisableGestureMonitor;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisableLowApiCheckForB;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisableMiuiLite;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisableMiuiWatermark;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisablePersistent;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisablePinVerifyPer72h;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisableRemoveFingerprintSensorConfig;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisableThermal;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.DisableVerifyCanBeDisabled;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.EffectBinderProxy;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.FlagSecure;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.FreeformBubble;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.GMSDozeFixFramework;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.HookEntry;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.LinkTurboToast;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.NoAccessDeviceLogsRequest;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.PackagePermissions;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.PstedClipboard;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.QuickScreenshot;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.ScreenRotation;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.SpeedInstall;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.ThermalBrightness;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.UseAndroidPackageInstaller;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.corepatch.AllowUpdateSystemApp;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.corepatch.BypassIsolationViolation;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.corepatch.BypassSignCheckForT;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.display.AllDarkMode;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.display.DisplayCutout;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.display.EnhanceRecentsVisibility;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.display.ThemeProvider;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.display.UseAOSPScreenShot;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.freeform.DisableFreeformBlackList;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.freeform.FreeFormCount;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.freeform.UnForegroundPin;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.network.DualNRSupport;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.network.DualSASupport;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.network.N1Band;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.network.N28Band;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.network.N5N8Band;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.volume.VolumeDefaultStream;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.volume.VolumeDisableSafe;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.volume.VolumeFirstPress;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.volume.VolumeMediaSteps;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.volume.VolumeSteps;
import com.sevtinge.hyperceiler.hook.module.skip.GlobalActions;

@HookBase(targetPackage = "android", isPad = 2, targetSdk = 36)
public class SystemFrameworkB extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // 核心破解
        initHook(BypassSignCheckForT.INSTANCE,
            (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak") || mPrefsMap.getBoolean("system_framework_core_patch_disable_integrity"))
            && mPrefsMap.getBoolean("system_framework_core_patch_enable")
        );
        initHook(new BypassIsolationViolation(), mPrefsMap.getBoolean("system_framework_core_patch_bypass_isolation_violation"));
        initHook(new AllowUpdateSystemApp(), mPrefsMap.getBoolean("system_framework_core_patch_allow_update_system_app"));
        initHook(new DisableLowApiCheckForB(), mPrefsMap.getBoolean("system_framework_disable_low_api_check"));
        initHook(new DisablePersistent(), mPrefsMap.getBoolean("system_framework_disable_persistent"));

        // 修复 A16 移植包开启核心破解后掉指纹，仅作备选项
        initHook(DisableRemoveFingerprintSensorConfig.INSTANCE, mPrefsMap.getBoolean("system_framework_core_patch_unloss_fingerprint"));

        // 手势初始化
        initHook(new PackagePermissions(), true);
        initHook(new GlobalActions(), true);

        // 小窗
        initHook(new FreeFormCount(), mPrefsMap.getBoolean("system_framework_freeform_count"));
        initHook(new DisableFreeformBlackList(), mPrefsMap.getBoolean("system_framework_disable_freeform_blacklist"));
        initHook(new FreeformBubble(), mPrefsMap.getBoolean("system_framework_freeform_bubble"));
        initHook(new UnForegroundPin(), mPrefsMap.getBoolean("system_framework_freeform_foreground_pin"));

        /*initHook(new StickyFloatingWindows(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));
        initHook(new AllowAutoStart(), mPrefsMap.getBoolean("system_framework_auto_start_apps_menu"));
        initHook(MultiFreeFormSupported.INSTANCE, mPrefsMap.getBoolean("system_framework_freeform_recents_to_small_freeform"));
        initHook(new OpenAppInFreeForm(), mPrefsMap.getBoolean("system_framework_freeform_jump"));*/

        // 音量
        initHook(new VolumeDefaultStream(), mPrefsMap.getStringAsInt("system_framework_default_volume_stream", 0) != 0);
        initHook(new VolumeFirstPress(), mPrefsMap.getBoolean("system_framework_volume_first_press"));
        initHook(new VolumeSteps(), mPrefsMap.getInt("system_framework_volume_steps", 0) > 0);
        initHook(new VolumeMediaSteps(), mPrefsMap.getBoolean("system_framework_volume_media_steps_enable"));
        initHook(new VolumeDisableSafe(), mPrefsMap.getStringAsInt("system_framework_volume_disable_safe_new", 0) != 0);

        // 显示
        initHook(new BackgroundBlur(), mPrefsMap.getBoolean("system_framework_background_blur_supported"));
        initHook(EnhanceRecentsVisibility.INSTANCE, mPrefsMap.getBoolean("system_framework_enhance_recents_visibility"));
        initHook(UseAOSPScreenShot.INSTANCE, mPrefsMap.getBoolean("system_ui_display_use_aosp_screenshot_enable"));
        initHook(new AllDarkMode(), mPrefsMap.getBoolean("system_framework_allow_all_dark_mode"));
        initHook(new ThemeProvider(), mPrefsMap.getBoolean("system_framework_allow_third_theme"));
        initHook(DisplayCutout.INSTANCE, mPrefsMap.getBoolean("system_ui_display_hide_cutout_enable"));

        // 网络
        initHook(DualNRSupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_nr"));
        initHook(DualSASupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_sa"));
        initHook(N1Band.INSTANCE, mPrefsMap.getBoolean("phone_n1"));
        initHook(N5N8Band.INSTANCE, mPrefsMap.getBoolean("phone_n5_n8"));
        initHook(N28Band.INSTANCE, mPrefsMap.getBoolean("phone_n28"));

        // 其它-显示与通知
        initHook(new ScreenRotation(), mPrefsMap.getBoolean("system_framework_screen_all_rotations"));
        initHook(new QuickScreenshot(), mPrefsMap.getBoolean("system_framework_quick_screenshot"));
        initHook(new AntiQues(), mPrefsMap.getBoolean("system_settings_anti_ques"));
        initHook(new DisablePinVerifyPer72h(), mPrefsMap.getBoolean("system_framework_disable_72h_verify"));
        initHook(new ThermalBrightness(), mPrefsMap.getBoolean("system_framework_other_thermal_brightness"));
        initHook(new AppLinkVerify(), mPrefsMap.getBoolean("system_framework_disable_app_link_verify"));
        initHook(NoAccessDeviceLogsRequest.INSTANCE, mPrefsMap.getBoolean("various_disable_access_device_logs"));
        initHook(new LinkTurboToast(), mPrefsMap.getBoolean("system_framework_disable_link_turbo_toast"));
        initHook(new FlagSecure(), mPrefsMap.getBoolean("system_other_flag_secure"));
        initHook(DeleteOnPostNotification.INSTANCE, mPrefsMap.getBoolean("system_other_delete_on_post_notification"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));

        // 其它-底层
        initHook(new DisableMiuiWatermark(), mPrefsMap.getBoolean("system_framework_disable_miui_watermark"));
        initHook(new SpeedInstall(), mPrefsMap.getBoolean("system_framework_other_speed_install"));
        initHook(new UseAndroidPackageInstaller(), mPrefsMap.getBoolean("system_framework_use_android_package_installer"));
        initHook(DisableGestureMonitor.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_gesture_monitor"));
        initHook(DisableThermal.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_thermal"));
        initHook(new HookEntry(), mPrefsMap.getBoolean("system_framework_hook_entry"));
        initHook(new DisableVerifyCanBeDisabled(), mPrefsMap.getBoolean("system_framework_disable_verify_can_ve_disabled"));
        initHook(new DisableMiuiLite(), mPrefsMap.getBoolean("system_framework_disablt_miuilite_check"));
        initHook(new PstedClipboard(), mPrefsMap.getBoolean("system_framework_posted_clipboard"));
        initHook(new ClipboardWhitelist(), mPrefsMap.getBoolean("system_framework_clipboard_whitelist"));
        initHook(new AllowDisableProtectedPackage(), mPrefsMap.getBoolean("system_framework_allow_disable_protected_package"));
        initHook(new BypassUnknownSourcesRestrictions(), mPrefsMap.getBoolean("system_framework_bypass_unknown_sources_restrictions"));
        initHook(new BypassForceMiAppStore(), mPrefsMap.getBoolean("system_framework_bypass_force_mi_appstore") || mPrefsMap.getBoolean("system_framework_market_use_detailmini"));
        initHook(new BypassForceDownloadui(), mPrefsMap.getBoolean("system_framework_bypass_force_downloadui"));
        initHook(ConservativeMilletFramework.INSTANCE, mPrefsMap.getBoolean("powerkeeper_conservative_millet"));
        initHook(GMSDozeFixFramework.INSTANCE, mPrefsMap.getBoolean("powerkeeper_gms_doze_fix"));

        // 清理菜单
        initHook(new CleanShareMenu(), mPrefsMap.getBoolean("system_framework_clean_share_menu"));
        initHook(new CleanOpenMenu(), mPrefsMap.getBoolean("system_framework_clean_open_menu"));
        initHook(new CleanProcessTextMenu(), mPrefsMap.getBoolean("system_framework_clean_process_text_menu"));

        if (mPrefsMap.getBoolean("misound_bluetooth") && isHyperOSVersion(2f)) {
            initHook(new EffectBinderProxy());
            initHook(new AutoEffectSwitchForSystem());
        }
    }
}
