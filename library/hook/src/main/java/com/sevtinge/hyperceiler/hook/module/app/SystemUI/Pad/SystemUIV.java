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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Pad;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.ToastBlur;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.UnlockAlwaysOnDisplay;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.volume.VolumeMediaSteps;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.AutoCollapse;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.AutoSEffSwitchForSystemUi;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.DisableTransparent;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.NotificationFix;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.StickyFloatingWindowsForSystemUI;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.UiLockApp;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.UnimportantNotification;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.UnlockClipboard;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.ZenModeFix;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.MiuiStub;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.AllowAllThemesNotificationBlur;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.AutoDismissExpandedPopupsHook;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.ControlCenterStyle;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.DisableDeviceManaged;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.ExpandNotificationKt;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.FiveGTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.FixTilesList;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.GmsTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.HideDelimiter;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.MuteVisibleNotifications;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NewFlashLight;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationColor;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationImportanceHyperOSFix;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.OldWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.QQSGrid;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.QSColor;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.QSGrid;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.ReduceBrightColorsTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.RemoveNotifNumLimit;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.ShadeHeaderGradientBlur;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.SnowLeopardModeTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.SunlightMode;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.SunlightModeHigh;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.TaplusTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.CustomBackground;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewLayout;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewSize;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.UnlockCustomActions;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.u.MediaControlPanelBackgroundMix;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.u.MediaPicture;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.u.MediaSeekBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.AllowThirdLockScreenUseFace;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.ChargingCVP;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.DisableUnlockByBleToast;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.HideLockScreenStatusBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.HideLockscreenZenMode;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.LinkageAnimCustomer;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.LockScreenDoubleTapToSleep;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.ScramblePIN;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.navigation.HandleLineCustom;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.navigation.HideNavigationBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.navigation.NavigationCustom;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.navigation.RotationButton;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.BrightnessPct;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.DisableBottomBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.DisableInfinitymodeGesture;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.DisableMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.MonetThemeOverlay;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.NotificationFreeform;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.RemoveMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.NewPluginHelperKt;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.BlurEnable;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.DoubleTapToSleep;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.HideStatusBarBeforeScreenshot;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.NotificationIconColumns;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.SelectiveHideIconForAlarmClock;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.clock.StatusBarClockNew;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.BatteryStyle;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.DataSaverIcon;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.HideBatteryIcon;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.HideVoWiFiIcon;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.IconsFromSystemManager;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.StatusBarIcon;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.WifiNetworkIndicator;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v.FocusNotifLyric;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v.WifiStandard;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeed;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeedStyle;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.strongtoast.HideStrongToast;
import com.sevtinge.hyperceiler.hook.module.skip.StatusBarActions;

@HookBase(targetPackage = "com.android.systemui", isPad = 1, targetSdk = 35)
public class SystemUIV extends BaseModule {
    @Override
    public void handleLoadPackage() {
        MiuiStub.createHook();

        // PluginHelper
        initHook(NewPluginHelperKt.INSTANCE);
        /*initHook(new NewPluginHelper());
        initHook(FocusNotifLyricPluginHelper.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_music_switch"));*/
        // initHook(Island.INSTANCE, true); // 灵动岛
        // initHook(DisableChargeAnimation.INSTANCE);

        // 小窗
        initHook(new NotificationFreeform(), mPrefsMap.getBoolean("system_ui_notification_freeform"));

        // Monet
        initHook(new MonetThemeOverlay(), mPrefsMap.getBoolean("system_ui_monet_overlay_custom"));

        // 状态栏图标
        initHook(new StatusBarIcon(), true);
        initHook(new IconsFromSystemManager(), true);
        initHook(WifiStandard.INSTANCE, mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0) > 0);
        initHook(new SelectiveHideIconForAlarmClock(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0) == 3 && mPrefsMap.getInt("system_ui_status_bar_icon_alarm_clock_n", 0) > 0);
        initHook(new NotificationIconColumns(), mPrefsMap.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(new HideStatusBarBeforeScreenshot(), mPrefsMap.getBoolean("system_ui_status_bar_hide_icon"));
        initHook(new DataSaverIcon(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_data_saver", 0) != 0);
        initHook(WifiNetworkIndicator.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_icon_wifi_network_indicator_new"));
        initHook(HideVoWiFiIcon.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi") || mPrefsMap.getBoolean("system_ui_status_bar_icon_volte"));
        initHook(new StickyFloatingWindowsForSystemUI(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));

        // 电池相关
        boolean isHideBatteryIcon = mPrefsMap.getBoolean("system_ui_status_bar_battery_icon") ||
                mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") ||
                mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark") ||
                mPrefsMap.getBoolean("system_ui_status_bar_battery_charging");
        initHook(HideBatteryIcon.INSTANCE, isHideBatteryIcon);
        initHook(BatteryStyle.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_battery_style_enable_custom") ||
                mPrefsMap.getBoolean("system_ui_status_bar_battery_style_change_location"));
        // initHook(new BatteryIndicator(), mPrefsMap.getBoolean("system_ui_status_bar_battery_indicator_enable"));

        // 网速指示器
        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_all_status_enable")) {
            initHook(NewNetworkSpeed.INSTANCE, true);
            initHook(NewNetworkSpeedStyle.INSTANCE, true);
            initHook(new NetworkSpeedSpacing(), mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing", 3) != 3);
            initHook(new NetworkSpeedSec(), mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit"));
        }

        // 时钟指示器
        initHook(StatusBarClockNew.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_clock_all_status_enable"));

        // initHook(new DisplayHardwareDetailForHyper(), true);

        // 焦点歌词
        initHook(FocusNotifLyric.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_music_switch"));

        // 灵动舞台
        initHook(HideStrongToast.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_hide_smart_strong_toast"));

        // 导航栏
        initHook(new HandleLineCustom(), mPrefsMap.getBoolean("system_ui_navigation_handle_custom"));
        initHook(new NavigationCustom(), mPrefsMap.getBoolean("system_ui_navigation_custom"));
        initHook(new HideNavigationBar(), mPrefsMap.getBoolean("system_ui_hide_navigation_bar"));
        initHook(new RotationButton(), mPrefsMap.getStringAsInt("system_framework_other_rotation_button_int", 0) != 0);

        // 实验性功能
        // initHook(new SwitchControlPanel(), false);
        // initHook(new MiuiGxzwSize(), false);

        // 通知与控制中心
        // initHook(new SmartHome(), false);
        initHook(new ShadeHeaderGradientBlur(), mPrefsMap.getBoolean("system_ui_shade_header_gradient_blur"));
        initHook(new QSColor(), mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") || mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color"));
        initHook(new UnimportantNotification(), mPrefsMap.getBoolean("system_ui_control_center_unimportant_notification"));
        initHook(new BlurEnable(), mPrefsMap.getBoolean("system_ui_control_center_statusbar_blur"));
        initHook(ExpandNotificationKt.INSTANCE, !mPrefsMap.getStringSet("system_ui_control_center_expand_notification").isEmpty());
        initHook(AutoDismissExpandedPopupsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_auto_clean_expand_notification"));
        initHook(new HideDelimiter(), mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) != 0);
        initHook(new GmsTile(), mPrefsMap.getBoolean("security_center_gms_open"));
        initHook(new TaplusTile(), mPrefsMap.getBoolean("security_center_taplus"));
        initHook(new ReduceBrightColorsTile(), mPrefsMap.getBoolean("security_center_reduce_bright_colors_tile"));
        initHook(new FiveGTile(), mPrefsMap.getStringAsInt("system_control_center_5g_new_tile", 0) != 0);
        initHook(new SnowLeopardModeTile(), mPrefsMap.getBoolean("system_ui_control_center_snow_leopard_mode"));
        initHook(NewFlashLight.INSTANCE, mPrefsMap.getStringAsInt("security_flash_light_switch", 0) != 0);
        if (mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode_high", 0) != 0) {
            initHook(new SunlightModeHigh());
        } else {
            initHook(new SunlightMode(), mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode", 0) != 0);
        }
        initHook(new MuteVisibleNotifications(), mPrefsMap.getBoolean("system_ui_control_center_mute_visible_notice"));
        initHook(OldWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(new QSGrid(), mPrefsMap.getBoolean("system_control_center_old_enable"));
        initHook(new QQSGrid(), mPrefsMap.getBoolean("system_control_center_old_enable"));
        initHook(new AutoCollapse(), mPrefsMap.getBoolean("system_ui_control_auto_close"));
        // initHook(RedirectToNotificationChannelSetting.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_redirect_notice"));
        initHook(ControlCenterStyle.INSTANCE, mPrefsMap.getBoolean("system_control_center_unlock_old"));
        initHook(NotificationImportanceHyperOSFix.INSTANCE, mPrefsMap.getBoolean("system_settings_more_notification_settings"));
        initHook(new FixTilesList(), mPrefsMap.getBoolean("system_ui_control_center_fix_tiles_list"));
        initHook(new AllowAllThemesNotificationBlur(), mPrefsMap.getBoolean("system_ui_control_center_unlock_blur_supported"));
        initHook(new DisableTransparent(), mPrefsMap.getBoolean("system_ui_control_center_notification_disable_transparent"));
        initHook(new DisableDeviceManaged(), mPrefsMap.getBoolean("system_ui_control_center_disable_device_managed"));
        initHook(new RemoveNotifNumLimit(), mPrefsMap.getBoolean("system_ui_control_center_remove_notif_num_limit"));
        initHook(new NotificationColor(), mPrefsMap.getBoolean("system_ui_control_center_opt_notification_element_background_color"));
        initHook(new ZenModeFix(), mPrefsMap.getBoolean("system_ui_control_center_zen_fix"));

        // Media Card
        initHook(MediaControlBgFactory.INSTANCE, mPrefsMap.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) != 0);
        initHook(CustomBackground.INSTANCE, mPrefsMap.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) != 0);
        initHook(new MediaControlPanelBackgroundMix(), mPrefsMap.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) == 5);
        initHook(new UnlockCustomActions(), mPrefsMap.getBoolean("system_ui_control_center_media_control_unlock_custom_actions"));
        initHook(MediaViewLayout.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_media_control_media_button_layout_switch"));
        initHook(MediaViewSize.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_media_control_media_button_size_switch"));
        initHook(MediaPicture.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_media_control_album_picture_rounded_corners") ||
                mPrefsMap.getBoolean("system_ui_control_center_media_control_remove_album_audio_source_identifie"));
        initHook(MediaSeekBar.INSTANCE, mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_color", -1) != -1
            || mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1) != -1 ||
            mPrefsMap.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) == 5 ||
                mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) != 0);

        // Actions
        initHook(new StatusBarActions(), true);

        // Other
        initHook(new UiLockApp(), mPrefsMap.getBoolean("system_framework_guided_access"));
        initHook(new NotificationFix(), mPrefsMap.getBoolean("system_ui_other_notification_fix"));
        initHook(new BrightnessPct(), mPrefsMap.getBoolean("system_showpct_title"));
        initHook(DisableMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_miui_multi_win_switch"));
        initHook(RemoveMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_remove_miui_multi_win_switch"));
        initHook(DisableInfinitymodeGesture.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_infinitymode_gesture"));
        initHook(DisableBottomBar.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_bottombar"));
        initHook(UnlockClipboard.INSTANCE, mPrefsMap.getBoolean("system_ui_unlock_clipboard"));

        initHook(new ToastBlur(), mPrefsMap.getBoolean("system_framework_background_blur_toast"));
        initHook(new UnlockAlwaysOnDisplay(), mPrefsMap.getBoolean("aod_unlock_always_on_display_hyper"));
        initHook(new VolumeMediaSteps(), mPrefsMap.getBoolean("system_framework_volume_media_steps_enable"));

        // 锁屏
        initHook(new ScramblePIN(), mPrefsMap.getBoolean("system_ui_lock_screen_scramble_pin"));
        initHook(ChargingCVP.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_show_charging_cv"));
        initHook(LockScreenDoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_double_lock"));
        initHook(HideLockscreenZenMode.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_not_disturb_mode"));
        initHook(HideLockScreenStatusBar.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_hide_status_bar"));
        initHook(AllowThirdLockScreenUseFace.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_allow_third_face"));
        initHook(new DisableUnlockByBleToast(), mPrefsMap.getBoolean("system_ui_lock_screen_disable_unlock_by_ble_toast"));
        initHook(new LinkageAnimCustomer(), mPrefsMap.getBoolean("system_ui_lock_screen_linkage_anim"));

        initHook(DoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_double_tap_to_sleep"));

        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));

        initHook(new AutoSEffSwitchForSystemUi(), mPrefsMap.getBoolean("misound_bluetooth"));
    }
}
