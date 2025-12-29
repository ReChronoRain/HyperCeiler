package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Pad;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.rules.systemframework.volume.VolumeMediaSteps;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.AutoCollapse;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.AutoSEffSwitchForSystemUi;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.DisableTransparent;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.UnlockClipboard;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.ZenModeFix;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.base.controlcenter.MediaControlBgFactory;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.AutoDismissExpandedPopupsHook;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.ControlCenterStyle;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.ExpandNotificationKt;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.FixTilesList;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.GmsTile;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.MuteVisibleNotifications;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.NewFlashLight;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.NotificationImportanceHyperOSFix;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.OldWeather;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.RedirectToNotificationChannelSetting;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.ReduceBrightColorsTile;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.SnowLeopardModeTile;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.SunlightMode;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.SunlightModeHigh;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.UnimportantNotification;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.media.CustomBackground;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.media.MediaViewLayout;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.media.MediaViewSize;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.media.UnlockCustomActions;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.media.b.MediaPicture;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.controlcenter.media.b.MediaSeekBar;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.AllowThirdLockScreenUseFace;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.DisableUnlockByBleToast;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.HideLockScreenHint;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.HideLockScreenStatusBar;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.HideLockscreenZenMode;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.KeepNotification;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.LockScreenDoubleTapToSleep;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.NotificationShowOnKeyguard;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.lockscreen.ScramblePIN;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.navigation.RotationButtonB;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.BrightnessPct;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.DisableBottomBar;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.DisableInfinitymodeGesture;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.DisableMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.MonetThemeOverlay;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.NotificationFreeform;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.other.RemoveMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.NewPluginHelperKt;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.plugin.systemui.QSColor;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.DoubleTapToSleep;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.HideStatusBarBeforeScreenshot;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.NotificationIconColumns;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.SelectiveHideIconForAlarmClock;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.clock.StatusBarClockNew;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.all.BatteryStyle;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.all.HideVoWiFiIcon;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.all.IconsFromSystemManager;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.all.StatusBarIcon;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.all.WifiNetworkIndicator;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.b.HideBatteryIconB;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.v.FocusNotifLyric;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.icon.v.WifiStandard;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.network.NewNetworkSpeed;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.network.NewNetworkSpeedStyle;
import com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.strongtoast.HideStrongToast;
import com.sevtinge.hyperceiler.hook.module.skip.StatusBarActions;

@HookBase(targetPackage = "com.android.systemui", isPad = 1, targetSdk = 36)
public class SystemUIB extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // PluginHelper
        initHook(NewPluginHelperKt.INSTANCE);
        // Actions
        initHook(new StatusBarActions(), true);

        // 锁屏
        initHook(HideLockScreenHint.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_unlock_tip"));
        initHook(HideLockScreenStatusBar.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_hide_status_bar"));
        initHook(NotificationShowOnKeyguard.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_unlock_notification_restrict"));
        initHook(KeepNotification.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_keep_notification"));
        initHook(HideLockscreenZenMode.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_not_disturb_mode"));
        initHook(new ScramblePIN(), mPrefsMap.getBoolean("system_ui_lock_screen_scramble_pin"));
        initHook(AllowThirdLockScreenUseFace.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_allow_third_face"));
        initHook(new DisableUnlockByBleToast(), mPrefsMap.getBoolean("system_ui_lock_screen_disable_unlock_by_ble_toast"));
        initHook(LockScreenDoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_double_lock"));

        // 状态栏图标
        initHook(new StatusBarIcon(), true);
        initHook(new IconsFromSystemManager(), true);
        initHook(WifiStandard.INSTANCE, mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0) > 0);
        initHook(WifiNetworkIndicator.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_icon_wifi_network_indicator_new"));
        initHook(HideVoWiFiIcon.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi") || mPrefsMap.getBoolean("system_ui_status_bar_icon_volte"));
        initHook(new SelectiveHideIconForAlarmClock(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0) == 3 && mPrefsMap.getInt("system_ui_status_bar_icon_alarm_clock_n", 0) > 0);
        initHook(new NotificationIconColumns(), mPrefsMap.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(NotificationImportanceHyperOSFix.INSTANCE, mPrefsMap.getBoolean("system_settings_more_notification_settings"));

        // 电池相关
        boolean isHideBatteryIcon = mPrefsMap.getBoolean("system_ui_status_bar_battery_icon") ||
            mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") ||
            mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark") ||
            mPrefsMap.getBoolean("system_ui_status_bar_battery_charging");
        initHook(HideBatteryIconB.INSTANCE, isHideBatteryIcon);
        initHook(BatteryStyle.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_battery_style_enable_custom") ||
            mPrefsMap.getBoolean("system_ui_status_bar_battery_style_change_location"));

        // 网速指示器
        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_all_status_enable")) {
            initHook(NewNetworkSpeed.INSTANCE, true);
            initHook(NewNetworkSpeedStyle.INSTANCE, true);
            initHook(new NetworkSpeedSpacing(), mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacings", 40) != 40);
            initHook(new NetworkSpeedSec(), mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit"));
        }

        // 时钟指示器
        initHook(StatusBarClockNew.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_clock_all_status_enable"));

        // 焦点歌词
        initHook(FocusNotifLyric.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_music_switch") && isHyperOSVersion(2f));

        // 灵动舞台
        initHook(HideStrongToast.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_hide_smart_strong_toast"));


        // 导航栏
        initHook(RotationButtonB.INSTANCE, mPrefsMap.getStringAsInt("system_framework_other_rotation_button_int", 0) != 0);

        // 控制与通知中心
        initHook(new QSColor(), mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") || mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color"));
        initHook(OldWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(AutoDismissExpandedPopupsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_auto_clean_expand_notification"));
        initHook(ExpandNotificationKt.INSTANCE, !mPrefsMap.getStringSet("system_ui_control_center_expand_notification").isEmpty());
        initHook(new UnimportantNotification(), mPrefsMap.getBoolean("system_ui_control_center_unimportant_notification"));
        initHook(RedirectToNotificationChannelSetting.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_redirect_notice"));
        initHook(new MuteVisibleNotifications(), mPrefsMap.getBoolean("system_ui_control_center_mute_visible_notice"));
        initHook(new ZenModeFix(), mPrefsMap.getBoolean("system_ui_control_center_zen_fix"));
        initHook(new DisableTransparent(), mPrefsMap.getBoolean("system_ui_control_center_notification_disable_transparent"));
        initHook(ControlCenterStyle.INSTANCE, mPrefsMap.getBoolean("system_control_center_unlock_old"));

        // 磁贴
        initHook(new AutoCollapse(), mPrefsMap.getBoolean("system_ui_control_auto_close"));
        initHook(new SnowLeopardModeTile(), mPrefsMap.getBoolean("system_ui_control_center_snow_leopard_mode"));
        initHook(new GmsTile(), mPrefsMap.getBoolean("security_center_gms_open"));
        // initHook(new TaplusTile(), mPrefsMap.getBoolean("security_center_taplus"));
        initHook(new ReduceBrightColorsTile(), mPrefsMap.getBoolean("security_center_reduce_bright_colors_tile"));
        initHook(NewFlashLight.INSTANCE, mPrefsMap.getStringAsInt("security_flash_light_switch", 0) != 0);
        if (mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode_high", 0) != 0) {
            initHook(new SunlightModeHigh());
        } else {
            initHook(new SunlightMode(), mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode", 0) != 0);
        }
        initHook(new FixTilesList(), mPrefsMap.getBoolean("system_ui_control_center_fix_tiles_list"));

        // Media Card
        initHook(new UnlockCustomActions(), mPrefsMap.getBoolean("system_ui_control_center_media_control_unlock_custom_actions"));
        initHook(MediaControlBgFactory.INSTANCE, mPrefsMap.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) != 0);
        initHook(CustomBackground.INSTANCE, mPrefsMap.getStringAsInt("system_ui_control_center_media_control_background_mode", 0) != 0);
        initHook(MediaViewLayout.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_media_control_media_button_layout_switch"));
        initHook(MediaViewSize.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_media_control_media_button_size_switch"));
        initHook(MediaPicture.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_media_control_album_picture_rounded_corners") ||
            mPrefsMap.getStringAsInt("system_ui_control_center_media_control_media_album_mode", 0) == 1);
        initHook(MediaSeekBar.INSTANCE, mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_color", -1) != -1
            || mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1) != -1
            || mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) != 0
            || mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_thumb_mode", 0) != 0);

        // Other
        initHook(DoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_double_tap_to_sleep"));
        initHook(new HideStatusBarBeforeScreenshot(), mPrefsMap.getBoolean("system_ui_status_bar_hide_icon"));

        initHook(new MonetThemeOverlay(), mPrefsMap.getBoolean("system_ui_monet_overlay_custom"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));
        initHook(new NotificationFreeform(), mPrefsMap.getBoolean("system_ui_notification_freeform"));
        initHook(new BrightnessPct(), mPrefsMap.getBoolean("system_showpct_title"));
        initHook(DisableMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_miui_multi_win_switch"));
        initHook(RemoveMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_remove_miui_multi_win_switch"));
        initHook(DisableInfinitymodeGesture.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_infinitymode_gesture"));
        initHook(DisableBottomBar.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_bottombar"));
        initHook(UnlockClipboard.INSTANCE, mPrefsMap.getBoolean("system_ui_unlock_clipboard"));

        initHook(new VolumeMediaSteps(), mPrefsMap.getBoolean("system_framework_volume_media_steps_enable"));
        if (mPrefsMap.getBoolean("misound_bluetooth") && isHyperOSVersion(2f)) {
            initHook(new AutoSEffSwitchForSystemUi().onApplication());
        }
    }
}
