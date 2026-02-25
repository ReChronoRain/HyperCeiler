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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.app.SystemUI;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.home.navigation.HideNavigationBar;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.others.UnlockAlwaysOnDisplay;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.volume.VolumeMediaSteps;
import com.sevtinge.hyperceiler.libhook.rules.systemsettings.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.libhook.rules.systemui.StatusBarActions;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.AutoDismissExpandedPopupsHook;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.BlurEnable;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.ControlCenterStyle;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.DisableDeviceManaged;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.DisableTransparent;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.ExpandNotificationKt;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.HideDelimiter;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.MuteVisibleNotifications;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.NotificationColor;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.NotificationImportanceHyperOSFix;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.OldWeather;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.QQSGrid;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.QSGrid;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.RedirectToNotificationChannelSetting;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.RemoveNotifNumLimit;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.ShadeHeaderGradientBlur;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.UnimportantNotification;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.ZenModeFix;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2.CustomBackground;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2.MediaViewLayout;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2.MediaViewSize;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2.UnlockCustomActions;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2.u.MediaControlPanelBackgroundMix;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2.u.MediaPicture;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2.u.MediaSeekBar;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.AutoCollapse;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.FiveGTile;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.FixTilesList;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.GmsTile;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.NewFlashLight;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.ReduceBrightColorsTile;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.SnowLeopardModeTile;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.SunlightModeTile;
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles.TaplusTile;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.AllowThirdLockScreenUseFace;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.BlurButton;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.ChargingCVP;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.CustomizeBottomButton;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.DisableUnlockByBleToast;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.HideLockScreenHint;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.HideLockScreenStatusBar;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.HideLockscreenZenMode;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.KeepNotification;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.LinkageAnimCustomer;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.LockScreenDoubleTapToSleep;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.NotificationShowOnKeyguard;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.RemoveCamera;
import com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen.ScramblePIN;
import com.sevtinge.hyperceiler.libhook.rules.systemui.navigation.HandleLineCustom;
import com.sevtinge.hyperceiler.libhook.rules.systemui.navigation.NavigationCustom;
import com.sevtinge.hyperceiler.libhook.rules.systemui.navigation.RotationButton;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.AutoSEffSwitchForSystemUi;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.BrightnessPct;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.DisableBottomBar;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.DisableInfinitymodeGesture;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.DisableMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.FuckStatusbarGestures;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.MonetThemeOverlay;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.NotificationFreeform;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.RemoveMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.ToastBlur;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.UiLockApp;
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.UnlockClipboard;
import com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.NewPluginHelperKt;
import com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui.QSColor;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.DoubleTapToSleep;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.HideStatusBarBeforeScreenshot;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.clock.StatusBarClockNew;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.BatteryStyle;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.DataSaverIcon;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.HideBatteryIcon;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.HideVoWiFiIcon;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.IconsFromSystemManager;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.NotificationIconColumns;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.SelectiveHideIconForAlarmClock;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.StatusBarIcon;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.SwapWiFiAndMobileNetwork;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all.WifiNetworkIndicator;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.v.WifiStandard;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.island.FocusNotifLyric;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.island.HideFakeStatusBar;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.island.HideStrongToast;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.DualRowSignalHookV;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.MobilePublicHookV;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.MobileTypeSingle2Hook;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.mobile.MobileTypeTextCustom;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network.NewNetworkSpeed;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network.NewNetworkSpeedStyle;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.Keyguard;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.MiuiStub;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.MediaControlBgFactory;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.util.Objects;

@HookBase(targetPackage = "com.android.systemui", maxSdk = 35)
public class SystemUIV extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        MiuiStub.createHook();
        // PluginHelper
        initHook(NewPluginHelperKt.INSTANCE);
        // Actions
        initHook(new StatusBarActions(), true);

        // 锁屏
        initHook(CustomizeBottomButton.INSTANCE, Keyguard.getLeftButtonType() != 0 && !isMoreSmallVersion(200, 2f));
        initHook(new ScramblePIN(), PrefsBridge.getBoolean("system_ui_lock_screen_scramble_pin"));
        initHook(ChargingCVP.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_show_charging_cv"));
        initHook(RemoveCamera.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_hide_camera") && !isMoreSmallVersion(200, 2f));
        initHook(LockScreenDoubleTapToSleep.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_double_lock"));
        initHook(NotificationShowOnKeyguard.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_unlock_notification_restrict"));
        initHook(KeepNotification.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_keep_notification"));
        initHook(HideLockscreenZenMode.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_not_disturb_mode"));
        initHook(HideLockScreenHint.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_unlock_tip"));
        initHook(HideLockScreenStatusBar.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_hide_status_bar"));
        initHook(AllowThirdLockScreenUseFace.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_allow_third_face"));
        initHook(new DisableUnlockByBleToast(), PrefsBridge.getBoolean("system_ui_lock_screen_disable_unlock_by_ble_toast"));
        initHook(new LinkageAnimCustomer(), PrefsBridge.getBoolean("system_ui_lock_screen_linkage_anim"));
        initHook(BlurButton.INSTANCE, PrefsBridge.getBoolean("system_ui_lock_screen_blur_button") && !isMoreSmallVersion(200, 2f));

        // 状态栏图标
        initHook(new StatusBarIcon(), true);
        initHook(new IconsFromSystemManager(), true);
        initHook(WifiStandard.INSTANCE, PrefsBridge.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0) > 0);
        initHook(new SelectiveHideIconForAlarmClock(), PrefsBridge.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0) == 3 && PrefsBridge.getInt("system_ui_status_bar_icon_alarm_clock_n", 0) > 0);
        initHook(new NotificationIconColumns(), PrefsBridge.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(new HideStatusBarBeforeScreenshot(), PrefsBridge.getBoolean("system_ui_status_bar_hide_icon"));
        initHook(new DataSaverIcon(), PrefsBridge.getStringAsInt("system_ui_status_bar_icon_data_saver", 0) != 0);
        initHook(WifiNetworkIndicator.INSTANCE, PrefsBridge.getBoolean("system_ui_status_bar_icon_wifi_network_indicator_new"));
        initHook(HideVoWiFiIcon.INSTANCE, PrefsBridge.getBoolean("system_ui_status_bar_icon_vowifi") || PrefsBridge.getBoolean("system_ui_status_bar_icon_volte"));
        // initHook(new StickyFloatingWindowsForSystemUI(), PrefsBridge.getBoolean("system_framework_freeform_sticky"));

        // 电池相关
        boolean isHideBatteryIcon = PrefsBridge.getBoolean("system_ui_status_bar_battery_icon") ||
                PrefsBridge.getBoolean("system_ui_status_bar_battery_percent") ||
                PrefsBridge.getBoolean("system_ui_status_bar_battery_percent_mark") ||
                PrefsBridge.getBoolean("system_ui_status_bar_battery_charging");
        initHook(HideBatteryIcon.INSTANCE, isHideBatteryIcon);
        initHook(BatteryStyle.INSTANCE, PrefsBridge.getBoolean("system_ui_status_bar_battery_style_enable_custom") ||
                PrefsBridge.getBoolean("system_ui_status_bar_battery_style_change_location"));

        // 网速指示器
        if (PrefsBridge.getBoolean("system_ui_statusbar_network_speed_all_status_enable")) {
            initHook(NewNetworkSpeed.INSTANCE, true);
            initHook(NewNetworkSpeedStyle.INSTANCE, true);
            initHook(new NetworkSpeedSpacing(), PrefsBridge.getInt("system_ui_statusbar_network_speed_update_spacings", 40) != 40);
            initHook(new NetworkSpeedSec(), PrefsBridge.getBoolean("system_ui_statusbar_network_speed_sec_unit"));
        }

        initHook(StatusBarClockNew.INSTANCE, PrefsBridge.getBoolean("system_ui_statusbar_clock_all_status_enable"));
        //
        // // 硬件指示器
        // initHook(new DisplayHardwareDetail(), PrefsBridge.getBoolean("system_ui_statusbar_battery_enable") ||
        //         PrefsBridge.getBoolean("system_ui_statusbar_temp_enable"));
        //
        // // initHook(new DisplayHardwareDetailForHyper(), true);

        // 焦点歌词
        if (PrefsBridge.getBoolean("system_ui_statusbar_music_switch") && isHyperOSVersion(2f)) {
            initHook(FocusNotifLyric.INSTANCE);
            initHook(HideFakeStatusBar.INSTANCE, PrefsBridge.getBoolean("system_ui_statusbar_music_hide_clock") && !isPad());
        }

        // 灵动舞台
        initHook(HideStrongToast.INSTANCE, PrefsBridge.getBoolean("system_ui_status_bar_hide_smart_strong_toast"));

        // 导航栏
        initHook(new HandleLineCustom(), PrefsBridge.getBoolean("system_ui_navigation_handle_custom"));
        initHook(new NavigationCustom(), PrefsBridge.getBoolean("system_ui_navigation_custom"));
        initHook(new HideNavigationBar(), PrefsBridge.getBoolean("system_ui_hide_navigation_bar"));
        initHook(new RotationButton(), PrefsBridge.getStringAsInt("system_framework_other_rotation_button_int", 0) != 0);

        // 通知与控制中心
        // initHook(new SmartHome(), false);
        initHook(new ShadeHeaderGradientBlur(), PrefsBridge.getBoolean("system_ui_shade_header_gradient_blur"));
        initHook(new QSColor(), PrefsBridge.getBoolean("system_ui_control_center_qs_open_color") || PrefsBridge.getBoolean("system_ui_control_center_qs_big_open_color"));
        initHook(new UnimportantNotification(), PrefsBridge.getBoolean("system_ui_control_center_unimportant_notification"));
        initHook(new BlurEnable(), PrefsBridge.getBoolean("system_ui_control_center_statusbar_blur"));
        initHook(ExpandNotificationKt.INSTANCE, !PrefsBridge.getStringSet("system_ui_control_center_expand_notification").isEmpty());
        initHook(AutoDismissExpandedPopupsHook.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_auto_clean_expand_notification"));
        initHook(new HideDelimiter(), PrefsBridge.getStringAsInt("system_ui_control_center_hide_operator", 0) != 0);
        initHook(new GmsTile(), PrefsBridge.getBoolean("security_center_gms_open"));
        initHook(new TaplusTile(), PrefsBridge.getBoolean("security_center_taplus"));
        initHook(new ReduceBrightColorsTile(), PrefsBridge.getBoolean("security_center_reduce_bright_colors_tile"));
        initHook(new SnowLeopardModeTile(), PrefsBridge.getBoolean("system_ui_control_center_snow_leopard_mode"));
        initHook(NewFlashLight.INSTANCE, PrefsBridge.getStringAsInt("security_flash_light_switch", 0) != 0);
        initHook(new SunlightModeTile(),
            PrefsBridge.getStringAsInt("system_control_center_sunshine_new_mode_high", 0) != 0 ||
                PrefsBridge.getStringAsInt("system_control_center_sunshine_new_mode", 0) != 0
        );
        initHook(new MuteVisibleNotifications(), PrefsBridge.getBoolean("system_ui_control_center_mute_visible_notice"));
        initHook(OldWeather.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeather.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_show_weather"));
        initHook(new QSGrid(), PrefsBridge.getBoolean("system_control_center_old_enable"));
        initHook(new QQSGrid(), PrefsBridge.getBoolean("system_control_center_old_enable"));
        initHook(new AutoCollapse(), PrefsBridge.getBoolean("system_ui_control_auto_close"));
        initHook(RedirectToNotificationChannelSetting.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_redirect_notice"));
        initHook(ControlCenterStyle.INSTANCE, PrefsBridge.getBoolean("system_control_center_unlock_old"));
        initHook(NotificationImportanceHyperOSFix.INSTANCE, PrefsBridge.getBoolean("system_settings_more_notification_settings"));
        initHook(new FixTilesList(), PrefsBridge.getBoolean("system_ui_control_center_fix_tiles_list"));
        initHook(new DisableTransparent(), PrefsBridge.getBoolean("system_ui_control_center_notification_disable_transparent"));
        initHook(new DisableDeviceManaged(), PrefsBridge.getBoolean("system_ui_control_center_disable_device_managed"));
        initHook(new RemoveNotifNumLimit(), PrefsBridge.getBoolean("system_ui_control_center_remove_notif_num_limit"));
        initHook(new NotificationColor(), PrefsBridge.getBoolean("system_ui_control_center_opt_notification_element_background_color"));
        initHook(new ZenModeFix(), PrefsBridge.getBoolean("system_ui_control_center_zen_fix"));

        // Media Card
        initHook(MediaControlBgFactory.INSTANCE, PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) != 0);
        initHook(CustomBackground.INSTANCE, PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) != 0);
        initHook(new MediaControlPanelBackgroundMix(), PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) == 5);
        initHook(new UnlockCustomActions(), PrefsBridge.getBoolean("system_ui_control_center_media_control_unlock_custom_actions"));
        initHook(MediaViewLayout.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_media_control_media_button_layout_switch"));
        initHook(MediaViewSize.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_media_control_media_button_size_switch"));
        initHook(MediaPicture.INSTANCE, PrefsBridge.getBoolean("system_ui_control_center_media_control_album_picture_rounded_corners") ||
            PrefsBridge.getStringAsInt("system_ui_control_center_media_control_media_album_mode", 0) == 1);
        initHook(MediaSeekBar.INSTANCE, PrefsBridge.getInt("system_ui_control_center_media_control_seekbar_color", -1) != -1
            || PrefsBridge.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1) != -1 ||
            PrefsBridge.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) == 5 ||
                PrefsBridge.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) != 0);

        // Other
        initHook(DoubleTapToSleep.INSTANCE, PrefsBridge.getBoolean("system_ui_status_bar_double_tap_to_sleep"));
        initHook(new HideStatusBarBeforeScreenshot(), PrefsBridge.getBoolean("system_ui_status_bar_hide_icon"));

        initHook(new UiLockApp(), PrefsBridge.getBoolean("system_framework_guided_access"));
        initHook(new AllowManageAllNotifications(), PrefsBridge.getBoolean("system_framework_allow_manage_all_notifications"));
        initHook(new MonetThemeOverlay(), PrefsBridge.getBoolean("system_ui_monet_overlay_custom"));
        initHook(new BrightnessPct(), PrefsBridge.getBoolean("system_showpct_title"));
        initHook(new NotificationFreeform(), PrefsBridge.getBoolean("system_ui_notification_freeform"));
        initHook(DisableMiuiMultiWinSwitch.INSTANCE, PrefsBridge.getBoolean("system_ui_disable_miui_multi_win_switch"));
        initHook(RemoveMiuiMultiWinSwitch.INSTANCE, PrefsBridge.getBoolean("system_ui_remove_miui_multi_win_switch"));
        initHook(DisableBottomBar.INSTANCE, PrefsBridge.getBoolean("system_ui_disable_bottombar"));
        initHook(UnlockClipboard.INSTANCE, PrefsBridge.getBoolean("system_ui_unlock_clipboard"));
        initHook(new ToastBlur(), PrefsBridge.getBoolean("system_framework_background_blur_toast"));
        initHook(new UnlockAlwaysOnDisplay(), PrefsBridge.getBoolean("aod_unlock_always_on_display_hyper"));
        initHook(new VolumeMediaSteps(), PrefsBridge.getBoolean("system_framework_volume_media_steps_enable"));
        initHook(new FuckStatusbarGestures(), PrefsBridge.getBoolean("system_ui_move_log_to_miui"));

        if (PrefsBridge.getBoolean("misound_bluetooth")) {
            initHook(new AutoSEffSwitchForSystemUi());
        }

        if (isPad()) {
            isPadLoaded();
        } else {
            isPhoneLoaded();
        }
    }

    private void isPhoneLoaded() {
        initHook(SwapWiFiAndMobileNetwork.INSTANCE, PrefsBridge.getBoolean("system_ui_status_bar_swap_wifi_and_mobile_network"));
        // 移动网络图标
        boolean isEnabledDualRowSignal = PrefsBridge.getBoolean("system_ui_statusbar_network_icon_enable");
        initHook(new DualRowSignalHookV(), isEnabledDualRowSignal);
        initHook(new MobilePublicHookV(), isEnabledDualRowSignal ||
            PrefsBridge.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_1") ||
            PrefsBridge.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_2") ||
            PrefsBridge.getBoolean("system_ui_status_bar_mobile_hide_roaming_icon") ||
            PrefsBridge.getBoolean("system_ui_status_bar_mobile_indicator") ||
            PrefsBridge.getStringAsInt("system_ui_status_bar_icon_small_hd", 0) != 0 ||
            PrefsBridge.getStringAsInt("system_ui_status_bar_icon_big_hd", 0) != 0);
        initHook(MobileTypeSingle2Hook.INSTANCE, PrefsBridge.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0) != 0 ||
            PrefsBridge.getBoolean("system_ui_statusbar_mobile_type_enable"));
        initHook(MobileTypeTextCustom.INSTANCE, !Objects.equals(PrefsBridge.getString("system_ui_status_bar_mobile_type_custom", ""), ""));

        // 磁贴
        initHook(new FiveGTile(), PrefsBridge.getStringAsInt("system_control_center_5g_new_tile", 0) != 0);
    }

    private void isPadLoaded() {
        // Other
        initHook(DisableInfinitymodeGesture.INSTANCE, PrefsBridge.getBoolean("system_ui_disable_infinitymode_gesture"));
    }
}
