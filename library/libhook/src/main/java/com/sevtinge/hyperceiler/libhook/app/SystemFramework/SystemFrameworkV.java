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
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.DisableLowApiCheckForU;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch.DisablePersistent;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.AllDarkMode;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.BackgroundBlur;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.DisplayCutout;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.EnhanceRecentsVisibility;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.ThemeProvider;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.display.UseAOSPScreenShot;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.AllowAutoStart;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.DisableFreeformBlackList;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.FreeFormCount;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.FreeformBubble;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.OpenAppInFreeForm;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform.StickyFloatingWindows;
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
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AppLinkVerify;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AutoEffectSwitchForSystem;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.BypassForceDownloadui;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.BypassForceMiAppStore;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.BypassUnknownSourcesRestrictions;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.CleanOpenMenu;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.CleanProcessTextMenu;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.CleanShareMenu;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.ClipboardWhitelist;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DeleteOnPostNotification;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableCleaner;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableGestureMonitor;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableMiuiLite;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisablePinVerifyPer72h;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableThermal;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.DisableVerifyCanBeDisabled;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.EffectBinderProxy;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.HookEntry;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.LinkTurboToast;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.NoAccessDeviceLogsRequest;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.PstedClipboard;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.QuickScreenshot;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.ScreenRotation;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.SpeedInstall;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.SystemLockApp;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.ThermalBrightness;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.UseAndroidPackageInstaller;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeDefaultStream;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeDisableSafe;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeFirstPress;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeMediaSteps;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeSteps;
import com.sevtinge.hyperceiler.libhook.rules.systemsettings.AntiQues;

@HookBase(targetPackage = "system", maxSdk = 35)
public class SystemFrameworkV extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(new PackagePermissions(), true);
        initHook(new GlobalActions(), true);
        // initHook(new DisableMiuiWatermark(), mPrefsMap.getBoolean("system_framework_disable_miui_watermark"));

        // 核心破解
        initHook(BypassSignCheckForT.INSTANCE,
            (mPrefsMap.getBoolean("system_framework_core_patch_auth_creak") || mPrefsMap.getBoolean("system_framework_core_patch_disable_integrity"))
                && mPrefsMap.getBoolean("system_framework_core_patch_enable")
        );
        initHook(new BypassIsolationViolation(), mPrefsMap.getBoolean("system_framework_core_patch_bypass_isolation_violation"));
        initHook(new AllowUpdateSystemApp(), mPrefsMap.getBoolean("system_framework_core_patch_allow_update_system_app"));
        initHook(new DisableLowApiCheckForU(), mPrefsMap.getBoolean("system_framework_disable_low_api_check"));
        initHook(new DisablePersistent(), mPrefsMap.getBoolean("system_framework_disable_persistent"));

        // 小窗
        initHook(new AllowAutoStart(), mPrefsMap.getBoolean("system_framework_auto_start_apps_menu"));
        initHook(new FreeFormCount(), mPrefsMap.getBoolean("system_framework_freeform_count"));
        initHook(new FreeformBubble(), mPrefsMap.getBoolean("system_framework_freeform_bubble"));
        initHook(new DisableFreeformBlackList(), mPrefsMap.getBoolean("system_framework_disable_freeform_blacklist"));
        initHook(new StickyFloatingWindows(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));
        initHook(new OpenAppInFreeForm(), mPrefsMap.getBoolean("system_framework_freeform_jump"));
        initHook(new UnForegroundPin(), mPrefsMap.getBoolean("system_framework_freeform_foreground_pin"));
        // initHook(new OpenAppInFreeForm(), mPrefsMap.getBoolean("system_framework_freeform_jump"));

        // 音量
        initHook(new VolumeDefaultStream(), mPrefsMap.getStringAsInt("system_framework_default_volume_stream", 0) != 0);
        initHook(new VolumeFirstPress(), mPrefsMap.getBoolean("system_framework_volume_first_press"));
        initHook(new VolumeSteps(), mPrefsMap.getInt("system_framework_volume_steps", 0) > 0);
        initHook(new VolumeMediaSteps(), mPrefsMap.getBoolean("system_framework_volume_media_steps_enable"));
        initHook(new VolumeDisableSafe(), mPrefsMap.getStringAsInt("system_framework_volume_disable_safe_new", 0) != 0);

        // 显示
        initHook(DisplayCutout.INSTANCE, mPrefsMap.getBoolean("system_ui_display_hide_cutout_enable"));
        initHook(new BackgroundBlur(), mPrefsMap.getBoolean("system_framework_background_blur_supported"));
        initHook(EnhanceRecentsVisibility.INSTANCE, mPrefsMap.getBoolean("system_framework_enhance_recents_visibility"));
        initHook(UseAOSPScreenShot.INSTANCE, mPrefsMap.getBoolean("system_ui_display_use_aosp_screenshot_enable"));
        initHook(new AllDarkMode(), mPrefsMap.getBoolean("system_framework_allow_all_dark_mode"));
        initHook(new ThemeProvider(), mPrefsMap.getBoolean("system_framework_allow_third_theme"));

        // 其他
        initHook(new AntiQues(), mPrefsMap.getBoolean("system_settings_anti_ques"));
        initHook(new SystemLockApp(), mPrefsMap.getBoolean("system_framework_guided_access"));
        initHook(new ScreenRotation(), mPrefsMap.getBoolean("system_framework_screen_all_rotations"));
        initHook(new CleanShareMenu(), mPrefsMap.getBoolean("system_framework_clean_share_menu"));
        initHook(new CleanOpenMenu(), mPrefsMap.getBoolean("system_framework_clean_open_menu"));
        initHook(new CleanProcessTextMenu(), mPrefsMap.getBoolean("system_framework_clean_process_text_menu"));
        initHook(new AllowUntrustedTouchForU(), mPrefsMap.getBoolean("system_framework_allow_untrusted_touch"));
        initHook(new AppLinkVerify(), mPrefsMap.getBoolean("system_framework_disable_app_link_verify"));
        initHook(new SpeedInstall(), mPrefsMap.getBoolean("system_framework_other_speed_install"));
        initHook(DeleteOnPostNotification.INSTANCE, mPrefsMap.getBoolean("system_other_delete_on_post_notification"));
        initHook(NoAccessDeviceLogsRequest.INSTANCE, mPrefsMap.getBoolean("various_disable_access_device_logs"));
        initHook(new DisableMiuiLite(), mPrefsMap.getBoolean("system_framework_disablt_miuilite_check"));
        initHook(new HookEntry(), mPrefsMap.getBoolean("system_framework_hook_entry"));
        initHook(new PstedClipboard(), mPrefsMap.getBoolean("system_framework_posted_clipboard"));
        initHook(new AllowDisableProtectedPackage(), mPrefsMap.getBoolean("system_framework_allow_disable_protected_package"));
        // 允许应用后台读取剪切板
        initHook(new ClipboardWhitelist(), mPrefsMap.getBoolean("system_framework_clipboard_whitelist"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));
        initHook(new BypassUnknownSourcesRestrictions(), mPrefsMap.getBoolean("system_framework_bypass_unknown_sources_restrictions"));

        initHook(new BypassForceMiAppStore(), mPrefsMap.getBoolean("system_framework_bypass_force_mi_appstore") || mPrefsMap.getBoolean("system_framework_market_use_detailmini"));
        initHook(new BypassForceDownloadui(), mPrefsMap.getBoolean("system_framework_bypass_force_downloadui"));

        initHook(new ThermalBrightness(), mPrefsMap.getBoolean("system_framework_other_thermal_brightness"));
        initHook(DisableCleaner.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_cleaner"));
        initHook(DisableGestureMonitor.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_gesture_monitor"));
        initHook(DisableThermal.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_thermal"));
        initHook(new DisablePinVerifyPer72h(), mPrefsMap.getBoolean("system_framework_disable_72h_verify"));
        initHook(new DisableVerifyCanBeDisabled(), mPrefsMap.getBoolean("system_framework_disable_verify_can_ve_disabled"));
        initHook(new UseAndroidPackageInstaller(), mPrefsMap.getBoolean("system_framework_use_android_package_installer"));
        initHook(new QuickScreenshot(), mPrefsMap.getBoolean("system_framework_quick_screenshot"));
        initHook(new LinkTurboToast(), mPrefsMap.getBoolean("system_framework_disable_link_turbo_toast"));

        if (mPrefsMap.getBoolean("misound_bluetooth")) {
            initHook(new EffectBinderProxy());
            initHook(new AutoEffectSwitchForSystem());
        }

        if (isPad()) {
            // 小米/红米平板设置相关
            initHook(IgnoreStylusKeyGesture.INSTANCE, mPrefsMap.getBoolean("mipad_input_ingore_gesture"));
            initHook(RemoveStylusBluetoothRestriction.INSTANCE, mPrefsMap.getBoolean("mipad_input_disable_bluetooth_new"));
            initHook(RestoreEsc.INSTANCE, mPrefsMap.getBoolean("mipad_input_restore_esc"));
            initHook(SetGestureNeedFingerNum.INSTANCE, mPrefsMap.getBoolean("mipad_input_need_finger_num"));
        }
    }

}
