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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.app.SystemFramework;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.AllowUpdateSystemApp;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.BypassIsolationViolation;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.BypassSignCheckForT;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.DisableLowApiCheckForB;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.DisablePersistent;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.AllDarkMode;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.BackgroundBlur;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.DisplayCutout;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.EnhanceRecentsVisibility;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.ThemeProvider;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.UseAOSPScreenShot;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.DisableFreeformBlackList;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.FreeFormCount;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.FreeformBubble;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.UnForegroundPin;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.mipad.IgnoreStylusKeyGesture;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.mipad.RemoveStylusBluetoothRestriction;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.mipad.RestoreEsc;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.mipad.SetGestureNeedFingerNum;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload.GlobalActions;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload.PackagePermissions;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AllowDisableProtectedPackage;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AllowUntrustedTouchForU;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AntiQues;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AppLinkVerify;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AutoEffectSwitchForSystem;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.BypassForceDownloadui;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.BypassForceMiAppStore;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.BypassUnknownSourcesRestrictions;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.CleanOpenMenu;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.CleanProcessTextMenu;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.CleanShareMenu;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.ClipboardWhitelist;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.ConservativeMilletFramework;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DeleteOnPostNotification;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableGestureMonitor;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableMiuiLite;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableMiuiWatermark;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisablePinVerifyPer72h;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableRemoveFingerprintSensorConfig;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableThermal;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableVerifyCanBeDisabled;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.EffectBinderProxy;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.GMSDozeFixFramework;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.HookEntry;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.LinkTurboToast;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.NoAccessDeviceLogsRequest;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.PstedClipboard;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.QuickScreenshot;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.ScreenRotation;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.SpeedInstall;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.ThermalBrightness;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.UseAndroidPackageInstaller;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeDefaultStream;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeDisableSafe;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeFirstPress;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeMediaSteps;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeSteps;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "system", minSdk = 36)
public class SystemFrameworkB extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        // 核心破解
        initHook(BypassSignCheckForT.INSTANCE,
            (PrefsBridge.getBoolean("system_framework_core_patch_auth_creak") || PrefsBridge.getBoolean("system_framework_core_patch_disable_integrity"))
            && PrefsBridge.getBoolean("system_framework_core_patch_enable")
        );
        initHook(new BypassIsolationViolation(), PrefsBridge.getBoolean("system_framework_core_patch_bypass_isolation_violation"));
        initHook(new AllowUpdateSystemApp(), PrefsBridge.getBoolean("system_framework_core_patch_allow_update_system_app"));
        initHook(new DisableLowApiCheckForB(), PrefsBridge.getBoolean("system_framework_disable_low_api_check"));
        initHook(new DisablePersistent(), PrefsBridge.getBoolean("system_framework_disable_persistent"));

        // 手势初始化
        initHook(new PackagePermissions(), true);
        initHook(new GlobalActions(), true);

        // 修复 A16 移植包开启核心破解后掉指纹，仅作备选项
        initHook(DisableRemoveFingerprintSensorConfig.INSTANCE, mPrefsMap.getBoolean("system_framework_core_patch_unloss_fingerprint"));

        // 小窗
        initHook(new FreeFormCount(), PrefsBridge.getBoolean("system_framework_freeform_count"));
        initHook(new DisableFreeformBlackList(), PrefsBridge.getBoolean("system_framework_disable_freeform_blacklist"));
        initHook(new FreeformBubble(), PrefsBridge.getBoolean("system_framework_freeform_bubble"));
        initHook(new UnForegroundPin(), PrefsBridge.getBoolean("system_framework_freeform_foreground_pin"));
        /*initHook(new StickyFloatingWindows(), PrefsBridge.getBoolean("system_framework_freeform_sticky"));
        initHook(new AllowAutoStart(), PrefsBridge.getBoolean("system_framework_auto_start_apps_menu"));
        initHook(MultiFreeFormSupported.INSTANCE, PrefsBridge.getBoolean("system_framework_freeform_recents_to_small_freeform"));
        initHook(new OpenAppInFreeForm(), PrefsBridge.getBoolean("system_framework_freeform_jump"));*/

        // 音量
        initHook(new VolumeDefaultStream(), PrefsBridge.getStringAsInt("system_framework_default_volume_stream", 0) != 0);
        initHook(new VolumeFirstPress(), PrefsBridge.getBoolean("system_framework_volume_first_press"));
        initHook(new VolumeSteps(), PrefsBridge.getInt("system_framework_volume_steps", 0) > 0);
        initHook(new VolumeMediaSteps(), PrefsBridge.getBoolean("system_framework_volume_media_steps_enable"));
        initHook(new VolumeDisableSafe(), PrefsBridge.getStringAsInt("system_framework_volume_disable_safe_new", 0) != 0);

        // 显示
        initHook(new BackgroundBlur(), PrefsBridge.getBoolean("system_framework_background_blur_supported"));
        initHook(EnhanceRecentsVisibility.INSTANCE, PrefsBridge.getBoolean("system_framework_enhance_recents_visibility"));
        initHook(UseAOSPScreenShot.INSTANCE, PrefsBridge.getBoolean("system_ui_display_use_aosp_screenshot_enable"));
        initHook(new AllDarkMode(), PrefsBridge.getBoolean("system_framework_allow_all_dark_mode"));
        initHook(new ThemeProvider(), PrefsBridge.getBoolean("system_framework_allow_third_theme"));
        initHook(DisplayCutout.INSTANCE, PrefsBridge.getBoolean("system_ui_display_hide_cutout_enable"));

        // 其它-显示与通知
        initHook(new ScreenRotation(), PrefsBridge.getBoolean("system_framework_screen_all_rotations"));
        initHook(new QuickScreenshot(), PrefsBridge.getBoolean("system_framework_quick_screenshot"));
        initHook(new AntiQues(), PrefsBridge.getBoolean("system_settings_anti_ques"));
        initHook(new DisablePinVerifyPer72h(), PrefsBridge.getBoolean("system_framework_disable_72h_verify"));
        initHook(new ThermalBrightness(), PrefsBridge.getBoolean("system_framework_other_thermal_brightness"));
        initHook(new AppLinkVerify(), PrefsBridge.getBoolean("system_framework_disable_app_link_verify"));
        initHook(NoAccessDeviceLogsRequest.INSTANCE, PrefsBridge.getBoolean("various_disable_access_device_logs"));
        initHook(new LinkTurboToast(), PrefsBridge.getBoolean("system_framework_disable_link_turbo_toast"));
        initHook(new AllowUntrustedTouchForU(), PrefsBridge.getBoolean("system_framework_allow_untrusted_touch"));
        initHook(DeleteOnPostNotification.INSTANCE, PrefsBridge.getBoolean("system_other_delete_on_post_notification"));
        initHook(new AllowManageAllNotifications(), PrefsBridge.getBoolean("system_framework_allow_manage_all_notifications"));

        // 其它-底层
        initHook(new DisableMiuiWatermark(), PrefsBridge.getBoolean("system_framework_disable_miui_watermark"));
        initHook(new SpeedInstall(), PrefsBridge.getBoolean("system_framework_other_speed_install"));
        initHook(new UseAndroidPackageInstaller(), PrefsBridge.getBoolean("system_framework_use_android_package_installer"));
        initHook(DisableGestureMonitor.INSTANCE, PrefsBridge.getBoolean("system_framework_other_disable_gesture_monitor"));
        initHook(DisableThermal.INSTANCE, PrefsBridge.getBoolean("system_framework_other_disable_thermal"));
        initHook(new HookEntry(), PrefsBridge.getBoolean("system_framework_hook_entry"));
        initHook(new DisableVerifyCanBeDisabled(), PrefsBridge.getBoolean("system_framework_disable_verify_can_ve_disabled"));
        initHook(new DisableMiuiLite(), PrefsBridge.getBoolean("system_framework_disablt_miuilite_check"));
        initHook(new PstedClipboard(), PrefsBridge.getBoolean("system_framework_posted_clipboard"));
        initHook(new ClipboardWhitelist(), PrefsBridge.getBoolean("system_framework_clipboard_whitelist"));
        initHook(new AllowDisableProtectedPackage(), PrefsBridge.getBoolean("system_framework_allow_disable_protected_package"));
        initHook(new BypassUnknownSourcesRestrictions(), PrefsBridge.getBoolean("system_framework_bypass_unknown_sources_restrictions"));
        initHook(new BypassForceMiAppStore(), PrefsBridge.getBoolean("system_framework_bypass_force_mi_appstore") || PrefsBridge.getBoolean("system_framework_market_use_detailmini"));
        initHook(new BypassForceDownloadui(), PrefsBridge.getBoolean("system_framework_bypass_force_downloadui"));
        initHook(ConservativeMilletFramework.INSTANCE, PrefsBridge.getBoolean("powerkeeper_conservative_millet"));
        initHook(GMSDozeFixFramework.INSTANCE, PrefsBridge.getBoolean("powerkeeper_gms_doze_fix"));

        // 清理菜单
        initHook(new CleanShareMenu(), PrefsBridge.getBoolean("system_framework_clean_share_menu"));
        initHook(new CleanOpenMenu(), PrefsBridge.getBoolean("system_framework_clean_open_menu"));
        initHook(new CleanProcessTextMenu(), PrefsBridge.getBoolean("system_framework_clean_process_text_menu"));

        if (isPad()) {
            // 小米/红米平板设置相关
            initHook(IgnoreStylusKeyGesture.INSTANCE, PrefsBridge.getBoolean("mipad_input_ingore_gesture"));
            initHook(RemoveStylusBluetoothRestriction.INSTANCE, PrefsBridge.getBoolean("mipad_input_disable_bluetooth_new"));
            initHook(RestoreEsc.INSTANCE, PrefsBridge.getBoolean("mipad_input_restore_esc"));
            initHook(SetGestureNeedFingerNum.INSTANCE, PrefsBridge.getBoolean("mipad_input_need_finger_num"));
        }

        if (PrefsBridge.getBoolean("misound_bluetooth")) {
            initHook(new EffectBinderProxy());
            initHook(new AutoEffectSwitchForSystem());
        }
    }
}
