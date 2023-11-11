package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.systemui.AutoCollapse;
import com.sevtinge.hyperceiler.module.hook.systemui.BluetoothRestrict;
import com.sevtinge.hyperceiler.module.hook.systemui.ChargeAnimationStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.HideNavigationBar;
import com.sevtinge.hyperceiler.module.hook.systemui.MonetThemeOverlay;
import com.sevtinge.hyperceiler.module.hook.systemui.NotificationFix;
import com.sevtinge.hyperceiler.module.hook.systemui.NotificationFreeform;
import com.sevtinge.hyperceiler.module.hook.systemui.OriginChargeAnimation;
import com.sevtinge.hyperceiler.module.hook.systemui.QSDetailBackGround;
import com.sevtinge.hyperceiler.module.hook.systemui.StatusBarActions;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.AddBlurEffectToNotificationView;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CCGrid;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CompactNotificationsHook;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.ControlCenterStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.FiveGTile;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.FixMediaControlPanel;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.FlashLight;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.GmsTile;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.MuteVisibleNotifications;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationRowMenu;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationWeatherNew;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationWeatherOld;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QQSGrid;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QQSGridOld;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSControlDetailBackgroundAlpha;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSGrid;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSGridLabels;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSGridOld;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.RedirectToNotificationChannelSetting;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.SunlightMode;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.SwitchCCAndNotification;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.AddBlurEffectToLockScreen;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.BlurButton;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ChargingCVP;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ClockDisplaySeconds;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ForceClockUseSystemFontsHook;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.HideLockScreenHint;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.HideLockScreenStatusBar;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.HideLockscreenZenMode;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.LockScreenDoubleTapToSleep;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.NoPassword;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.RemoveCamera;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.RemoveSmartScreen;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ScramblePIN;
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.HandleLineCustom;
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.NavigationCustom;
import com.sevtinge.hyperceiler.module.hook.systemui.plugin.PluginHelper;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.DisplayHardwareDetail;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.DoubleTapToSleep;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.DualRowSignalHook;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.HideStatusBarBeforeScreenshot;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.MobileNetwork;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.NotificationIconColumns;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.SelectiveHideIconForAlarmClock;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.WifiStandard;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock.TimeCustomization;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock.TimeStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.BatteryStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.BluetoothIcon;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.DataSaverIcon;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.HideBatteryIcon;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.HideVoWiFiIcon;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.IconsFromSystemManager;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.StatusBarIcon;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.StatusBarIconPositionAdjust;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.StatusBarSimIcon;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all.WifiNetworkIndicator;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.t.UseNewHD;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.layout.StatusBarLayout;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.MobileTypeSingleHook;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.MobileTypeTextCustom;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.NetworkSpeed;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.NetworkSpeedStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.StatusBarNoNetSpeedSep;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.s.NetworkSpeedWidth;

import java.util.Objects;

public class SystemUI extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // 充电动画
        initHook(new ChargeAnimationStyle(), mPrefsMap.getStringAsInt("system_ui_charge_animation_style", 0) > 0);
        initHook(new OriginChargeAnimation(), mPrefsMap.getBoolean("system_ui_origin_charge_animation"));

        // 小窗
        initHook(new NotificationFreeform(), mPrefsMap.getBoolean("system_ui_notification_freeform"));

        // 禁用蓝牙临时关闭
        initHook(new BluetoothRestrict(), mPrefsMap.getBoolean("various_disable_bluetooth_restrict"));

        // Monet
        initHook(new MonetThemeOverlay(), mPrefsMap.getBoolean("system_ui_monet_overlay_custom"));

        // 状态栏图标
        initHook(WifiNetworkIndicator.INSTANCE, mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_network_indicator", 0) > 0);
        initHook(new StatusBarIcon());
        initHook(new IconsFromSystemManager());
        initHook(new WifiStandard(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0) > 0);
        initHook(new BluetoothIcon(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth", 0) != 0);
        initHook(new SelectiveHideIconForAlarmClock(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0) == 3 && mPrefsMap.getInt("system_ui_status_bar_icon_alarm_clock_n", 0) > 0);
        initHook(new NotificationIconColumns(), mPrefsMap.getBoolean("system_ui_status_bar_notification_dots_maximum_enable") || mPrefsMap.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(UseNewHD.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_use_new_hd"));
        initHook(new HideStatusBarBeforeScreenshot(), mPrefsMap.getBoolean("system_ui_status_bar_hide_icon"));
        initHook(StatusBarSimIcon.INSTANCE);
        initHook(HideVoWiFiIcon.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi") || mPrefsMap.getBoolean("system_ui_status_bar_icon_volte"));
        initHook(new DataSaverIcon(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_data_saver", 0) != 0);

        // 移动网络图标
        initHook(new MobileNetwork());
        // initHook(new BigMobileNetworkType(), false);
        initHook(new DualRowSignalHook(), mPrefsMap.getBoolean("system_ui_statusbar_network_icon_enable"));
        initHook(MobileTypeSingleHook.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable"));
        initHook(MobileTypeTextCustom.INSTANCE, !Objects.equals(mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", ""), ""));

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
        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_enable_custom")) {
            initHook(NetworkSpeed.INSTANCE, !isMoreAndroidVersion(34));
            initHook(NetworkSpeedWidth.INSTANCE, mPrefsMap.getInt("system_ui_statusbar_network_speed_fixedcontent_width", 10) > 10);
        }
        initHook(NetworkSpeedStyle.INSTANCE);
        initHook(new NetworkSpeedSpacing(), mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing", 3) != 3);
        initHook(new NetworkSpeedSec(), mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit"));
        initHook(StatusBarNoNetSpeedSep.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_no_netspeed_separator"));

        // 时钟指示器
        initHook(TimeStyle.INSTANCE);
        initHook(TimeCustomization.INSTANCE, mPrefsMap.getStringAsInt("system_ui_statusbar_clock_mode", 0) != 0);

        // 硬件指示器
        initHook(new DisplayHardwareDetail(), mPrefsMap.getBoolean("system_ui_statusbar_battery_enable") ||
            mPrefsMap.getBoolean("system_ui_statusbar_temp_enable"));

        // 居右显示
        boolean isWiFiAtLeft = mPrefsMap.getBoolean("system_ui_status_bar_wifi_at_left");
        boolean isMobileNetworkAtLeft = mPrefsMap.getBoolean("system_ui_status_bar_mobile_network_at_left");

        boolean isNetworkSpeedAtRight = mPrefsMap.getBoolean("system_ui_status_bar_network_speed_at_right");
        boolean isAlarmClockAtRight = mPrefsMap.getBoolean("system_ui_status_bar_alarm_clock_at_right");
        boolean isNFCAtRight = mPrefsMap.getBoolean("system_ui_status_bar_nfc_at_right");
        boolean isVolumeAtRight = mPrefsMap.getBoolean("system_ui_status_bar_volume_at_right");
        boolean isZenAtRight = mPrefsMap.getBoolean("system_ui_status_bar_zen_at_right");

        boolean isSwapWiFiAndMobileNetwork = mPrefsMap.getBoolean("system_ui_status_bar_swap_wifi_and_mobile_network");

        boolean isStatusBarIconAtRightEnable = isWiFiAtLeft || isMobileNetworkAtLeft || isSwapWiFiAndMobileNetwork || isNetworkSpeedAtRight || isAlarmClockAtRight || isNFCAtRight || isVolumeAtRight || isZenAtRight;

        initHook(new StatusBarIconPositionAdjust(), isStatusBarIconAtRightEnable);

        // 导航栏
        initHook(HandleLineCustom.INSTANCE, mPrefsMap.getBoolean("system_ui_navigation_handle_custom"));
        initHook(new NavigationCustom(), mPrefsMap.getBoolean("system_ui_navigation_custom"));
        initHook(new HideNavigationBar(), mPrefsMap.getBoolean("system_ui_hide_navigation_bar"));

        // 状态栏布局
        initHook(StatusBarLayout.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_layout_compatibility_mode") ||
            mPrefsMap.getStringAsInt("system_ui_statusbar_layout_mode", 0) != 0);

        // 实验性功能
        // initHook(new SwitchControlPanel(), false);
        // initHook(new MiuiGxzwSize(), false);

        // 控制中心
        // initHook(new SmartHome(), false);
        initHook(new QSDetailBackGround(), mPrefsMap.getInt("system_control_center_qs_detail_bg", 0) > 0);
        initHook(new GmsTile(), mPrefsMap.getBoolean("security_center_gms_open"));
        initHook(new FiveGTile(), mPrefsMap.getBoolean("system_control_center_5g_tile"));
        initHook(new FlashLight(), mPrefsMap.getBoolean("security_flash_light"));
        initHook(new SunlightMode(), mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode", 0) != 0);
        initHook(new QSGridLabels(), mPrefsMap.getInt("system_control_center_old_qs_row", 1) > 1 ||
            mPrefsMap.getBoolean("system_control_center_qs_tile_label"));
        initHook(new MuteVisibleNotifications(), mPrefsMap.getBoolean("system_ui_control_center_mute_visible_notice"));
        initHook(new SwitchCCAndNotification(), mPrefsMap.getBoolean("system_ui_control_center_switch_cc_and_notification"));
        initHook(QSControlDetailBackgroundAlpha.INSTANCE, mPrefsMap.getInt("system_ui_control_center_control_detail_background_alpha", 255) != 255);
        initHook(FixMediaControlPanel.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_fix_media_control_panel"));
        initHook(NotificationWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeatherOld.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeatherNew.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(CompactNotificationsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_compact_notice"));
        initHook(CCGrid.INSTANCE, mPrefsMap.getInt("system_control_center_cc_rows", 4) > 4 ||
            mPrefsMap.getInt("system_control_center_cc_columns", 4) > 4 ||
            mPrefsMap.getBoolean("system_ui_control_center_rounded_rect") ||
            mPrefsMap.getBoolean("system_control_center_qs_tile_label"));
        if (isMoreAndroidVersion(33)) {
            initHook(new QSGrid(), mPrefsMap.getBoolean("system_control_center_old_enable"));
            initHook(new QQSGrid(), mPrefsMap.getBoolean("system_control_center_old_enable"));
        } else {
            initHook(new QSGridOld(), mPrefsMap.getBoolean("system_control_center_old_enable_1"));
            initHook(new QQSGridOld(), mPrefsMap.getBoolean("system_control_center_old_enable_1"));
        }
        initHook(new AutoCollapse(), mPrefsMap.getBoolean("system_ui_control_auto_close"));
        initHook(RedirectToNotificationChannelSetting.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_redirect_notice"));
        initHook(ControlCenterStyle.INSTANCE, mPrefsMap.getBoolean("system_control_center_unlock_old"));
        initHook(new NotificationRowMenu(), mPrefsMap.getBoolean("system_ui_control_center_notifrowmenu"));

        // Actions
        initHook(new StatusBarActions());

        // Other
        initHook(new NotificationFix(), mPrefsMap.getBoolean("system_ui_other_notification_fix"));

        // 锁屏
        initHook(new ScramblePIN(), mPrefsMap.getBoolean("system_ui_lock_screen_scramble_pin"));
        initHook(ClockDisplaySeconds.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_show_second"));
        initHook(ChargingCVP.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_show_charging_cv"));
        initHook(RemoveCamera.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_hide_camera"));
        initHook(RemoveSmartScreen.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_hide_smart_screen"));
        initHook(NoPassword.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_password_free"));
        initHook(LockScreenDoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_double_lock"));
        initHook(ForceClockUseSystemFontsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_force_system_fonts"));
        initHook(HideLockscreenZenMode.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_not_disturb_mode"));
        initHook(HideLockScreenHint.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_unlock_tip"));
        initHook(HideLockScreenStatusBar.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_hide_status_bar"));

        if (!isAndroidVersion(30)) {
            initHook(AddBlurEffectToLockScreen.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_blur_button"));
            initHook(AddBlurEffectToNotificationView.INSTANCE, mPrefsMap.getBoolean("n_enable"));
            initHook(BlurButton.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_blur_button"));
        }

        initHook(DoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_double_tap_to_sleep"));

        initHook(new PluginHelper());
    }
}
