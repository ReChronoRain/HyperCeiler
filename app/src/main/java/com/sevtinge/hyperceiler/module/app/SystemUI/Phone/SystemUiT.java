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

package com.sevtinge.hyperceiler.module.app.SystemUI.Phone;

import static com.sevtinge.hyperceiler.utils.api.OldFunApisKt.isNewNetworkStyle;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.systemui.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.module.hook.systemui.AutoCollapse;
import com.sevtinge.hyperceiler.module.hook.systemui.BluetoothRestrict;
import com.sevtinge.hyperceiler.module.hook.systemui.BrightnessPct;
import com.sevtinge.hyperceiler.module.hook.systemui.ChargeAnimationStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.DisableBottomBar;
import com.sevtinge.hyperceiler.module.hook.systemui.DisableInfinitymodeGesture;
import com.sevtinge.hyperceiler.module.hook.systemui.DisableMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.module.hook.systemui.DisableTransparent;
import com.sevtinge.hyperceiler.module.hook.systemui.MonetThemeOverlay;
import com.sevtinge.hyperceiler.module.hook.systemui.NotificationFix;
import com.sevtinge.hyperceiler.module.hook.systemui.NotificationFreeform;
import com.sevtinge.hyperceiler.module.hook.systemui.QSDetailBackGround;
import com.sevtinge.hyperceiler.module.hook.systemui.RemoveMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.module.hook.systemui.SquigglyProgress;
import com.sevtinge.hyperceiler.module.hook.systemui.StatusBarActions;
import com.sevtinge.hyperceiler.module.hook.systemui.StickyFloatingWindowsForSystemUI;
import com.sevtinge.hyperceiler.module.hook.systemui.UiLockApp;
import com.sevtinge.hyperceiler.module.hook.systemui.UnimportantNotification;
import com.sevtinge.hyperceiler.module.hook.systemui.UnlockClipboard;
import com.sevtinge.hyperceiler.module.hook.systemui.UnlockCustomActions;
import com.sevtinge.hyperceiler.module.hook.systemui.VolumeTimerValuesHook;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.AddBlurEffectToNotificationView;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.AllowAllThemesNotificationBlur;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CCGrid;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CompactNotificationsHook;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.ControlCenterStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.DisableDeviceManaged;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.ExpandNotification;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.FiveGTile;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.FixTilesList;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.FlashLight;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.GmsTile;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.HideDelimiter;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.MediaButton;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.MediaControlPanelBackgroundMix;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.MediaControlPanelTimeViewTextSize;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.MediaSeekBarColor;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.MuteVisibleNotifications;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationImportanceHyperOSFix;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationRowMenu;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationWeatherNew;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.NotificationWeatherOld;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QQSGrid;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSColor;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSControlDetailBackgroundAlpha;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSGrid;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.QSGridLabels;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.RedirectToNotificationChannelSetting;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.ReduceBrightColorsTile;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.RemoveNotifNumLimit;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.SnowLeopardModeTile;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.SunlightMode;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.SunlightModeHigh;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.SwitchCCAndNotification;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.TaplusTile;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.AddBlurEffectToLockScreen;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.AllowThirdLockScreenUseFace;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.BlockEditor;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.BlurButton;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ChargingCVP;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ClockDisplaySeconds;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.DisableUnlockByBleToast;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ForceClockUseSystemFontsHook;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.HideLockScreenHint;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.HideLockScreenStatusBar;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.HideLockscreenZenMode;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.LinkageAnimCustomer;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.LockScreenDoubleTapToSleep;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.NoPassword;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.RemoveCamera;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.RemoveSmartScreen;
import com.sevtinge.hyperceiler.module.hook.systemui.lockscreen.ScramblePIN;
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.HandleLineCustom;
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.HideNavigationBar;
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.NavigationCustom;
import com.sevtinge.hyperceiler.module.hook.systemui.navigation.RotationButton;
import com.sevtinge.hyperceiler.module.hook.systemui.plugin.PluginHelper;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.BlurEnable;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.DoubleTapToSleep;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.HideStatusBarBeforeScreenshot;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.NotificationIconColumns;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.SelectiveHideIconForAlarmClock;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.WifiStandard;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock.DisableAnim;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock.FixColor;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock.StatusBarClockNew;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock.TimeCustomization;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock.TimeStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.device.DisplayHardwareDetail;
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
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.DualRowSignalHook;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.MobileNetwork;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.MobilePublicHook;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.MobileTypeSingleHook;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.MobileTypeTextCustom;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.news.NewNetworkSpeed;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.news.NewNetworkSpeedStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.old.NetworkSpeed;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.old.NetworkSpeedStyle;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.old.StatusBarNoNetSpeedSep;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.s.NetworkSpeedWidth;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.strongtoast.HideStrongToast;

import java.util.Objects;

@HookBase(targetPackage = "com.android.systemui", isPad = false, targetSdk = 33)
public class SystemUiT extends BaseModule {
    @Override
    public void handleLoadPackage() {
        // PluginHelper
        initHook(new PluginHelper(), true);// 充电动画
        // initHook(Island.INSTANCE, true); // 灵动岛
        initHook(new ChargeAnimationStyle(), mPrefsMap.getStringAsInt("system_ui_charge_animation_style", 0) > 0);
        // initHook(DisableChargeAnimation.INSTANCE);

        // 小窗
        initHook(new NotificationFreeform(), mPrefsMap.getBoolean("system_ui_notification_freeform"));

        // 禁用蓝牙临时关闭
        initHook(new BluetoothRestrict(), mPrefsMap.getBoolean("system_ui_disable_bluetooth_restrict"));

        // Monet
        initHook(new MonetThemeOverlay(), mPrefsMap.getBoolean("system_ui_monet_overlay_custom"));

        // 状态栏图标
        boolean isHideSim = (mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_1") ||
                mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_2")) &&
                !mPrefsMap.getBoolean("system_ui_statusbar_network_icon_enable") && !isMoreHyperOSVersion(1f);

        initHook(new StatusBarIcon(), true);
        initHook(new IconsFromSystemManager(), true);
        initHook(new UnlockCustomActions(), mPrefsMap.getBoolean("system_ui_control_center_media_control_unlock_custom_actions"));
        initHook(new MediaButton(), mPrefsMap.getInt("system_ui_control_center_media_control_media_button", 140) != 140
                || mPrefsMap.getInt("system_ui_control_center_media_control_media_button_custom", 140) != 140);
        initHook(new MediaSeekBarColor(), mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_color", -1) != -1
                || mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1) != -1);
        initHook(new SquigglyProgress(), mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) == 1);
        initHook(new MediaControlPanelTimeViewTextSize(), mPrefsMap.getInt("system_ui_control_center_media_control_time_view_text_size", 13) != 13);
        initHook(new BluetoothIcon(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth", 0) != 0 && !isMoreHyperOSVersion(1f));
        initHook(new WifiStandard(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0) > 0);
        initHook(new SelectiveHideIconForAlarmClock(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0) == 3 && mPrefsMap.getInt("system_ui_status_bar_icon_alarm_clock_n", 0) > 0);
        initHook(new NotificationIconColumns(), mPrefsMap.getBoolean("system_ui_status_bar_notification_dots_maximum_enable") || mPrefsMap.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(new HideStatusBarBeforeScreenshot(), mPrefsMap.getBoolean("system_ui_status_bar_hide_icon"));
        initHook(new DataSaverIcon(), mPrefsMap.getStringAsInt("system_ui_status_bar_icon_data_saver", 0) != 0);
        initHook(WifiNetworkIndicator.INSTANCE, mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_network_indicator", 0) > 0);
        initHook(StatusBarSimIcon.INSTANCE, isHideSim);
        initHook(HideVoWiFiIcon.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_icon_vowifi") || mPrefsMap.getBoolean("system_ui_status_bar_icon_volte"));
        initHook(UseNewHD.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_use_new_hd"));
        initHook(new StickyFloatingWindowsForSystemUI(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));

        // 移动网络图标
        boolean isEnableMobilePublic = mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_1") ||
                mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_2") ||
                mPrefsMap.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0) != 0 ||
                mPrefsMap.getBoolean("system_ui_status_bar_mobile_hide_roaming_icon") ||
                mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable") ||
                mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator");
        boolean isEnableMobileNetwork = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_small_hd", 0) != 0 ||
                mPrefsMap.getStringAsInt("system_ui_status_bar_icon_big_hd", 0) != 0 ||
                mPrefsMap.getStringAsInt("system_ui_status_bar_icon_new_hd", 0) != 0;

        initHook(MobilePublicHook.INSTANCE, isEnableMobilePublic);
        initHook(new MobileNetwork(), isEnableMobileNetwork);
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
        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_all_status_enable")) {
            if (!isNewNetworkStyle()) {
                initHook(NetworkSpeed.INSTANCE, true);
                initHook(NetworkSpeedWidth.INSTANCE, mPrefsMap.getInt("system_ui_statusbar_network_speed_fixedcontent_width", 10) > 10);
                initHook(NetworkSpeedStyle.INSTANCE, true);
                initHook(StatusBarNoNetSpeedSep.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_no_netspeed_separator"));
            } else {
                initHook(NewNetworkSpeed.INSTANCE, true);
                initHook(NewNetworkSpeedStyle.INSTANCE, true);
            }
            initHook(new NetworkSpeedSpacing(), mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing", 3) != 3);
            initHook(new NetworkSpeedSec(), mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit"));
        }

        // 时钟指示器
        boolean isEnableTime = mPrefsMap.getStringAsInt("system_ui_statusbar_clock_mode", 0) != 0 ||
                mPrefsMap.getInt("system_ui_statusbar_clock_left_margin", 0) != 0 ||
                mPrefsMap.getInt("system_ui_statusbar_clock_right_margin", 0) != 0 ||
                mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset", 12) != 12 ||
                mPrefsMap.getBoolean("system_ui_statusbar_clock_bold");

        if (isMoreHyperOSVersion(1f)) {
            initHook(StatusBarClockNew.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_clock_all_status_enable"));
            initHook(new DisableAnim(), mPrefsMap.getBoolean("system_ui_disable_clock_anim"));
            initHook(new FixColor(), mPrefsMap.getBoolean("system_ui_statusbar_clock_fix_color"));
        } else {
            initHook(TimeStyle.INSTANCE, isEnableTime);
            initHook(TimeCustomization.INSTANCE, mPrefsMap.getStringAsInt("system_ui_statusbar_clock_mode", 0) != 0);
        }

        // 硬件指示器
        initHook(new DisplayHardwareDetail(), mPrefsMap.getBoolean("system_ui_statusbar_battery_enable") ||
                mPrefsMap.getBoolean("system_ui_statusbar_temp_enable"));

        // initHook(new DisplayHardwareDetailForHyper(), true);

        // 灵动舞台
        initHook(HideStrongToast.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_hide_smart_strong_toast"));

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
        initHook(new HandleLineCustom(), mPrefsMap.getBoolean("system_ui_navigation_handle_custom"));
        initHook(new NavigationCustom(), mPrefsMap.getBoolean("system_ui_navigation_custom"));
        initHook(new HideNavigationBar(), mPrefsMap.getBoolean("system_ui_hide_navigation_bar"));
        initHook(new RotationButton(), mPrefsMap.getStringAsInt("system_framework_other_rotation_button_int", 0) != 0);
        // 状态栏布局
        initHook(StatusBarLayout.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_layout_compatibility_mode") ||
                mPrefsMap.getStringAsInt("system_ui_statusbar_layout_mode", 0) != 0);

        // 实验性功能
        // initHook(new SwitchControlPanel(), false);
        // initHook(new MiuiGxzwSize(), false);

        // 控制中心
        // initHook(new SmartHome(), false);
        initHook(new QSColor(), mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") || mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color"));
        initHook(new UnimportantNotification(), mPrefsMap.getBoolean("system_ui_control_center_unimportant_notification"));
        initHook(new BlurEnable(), mPrefsMap.getBoolean("system_ui_control_center_statusbar_blur"));
        initHook(new ExpandNotification(), !mPrefsMap.getStringSet("system_ui_control_center_expand_notification").isEmpty());
        initHook(new HideDelimiter(), mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) != 0);
        initHook(new QSDetailBackGround(), mPrefsMap.getInt("system_control_center_qs_detail_bg", 0) > 0);
        initHook(new GmsTile(), mPrefsMap.getBoolean("security_center_gms_open"));
        initHook(new TaplusTile(), mPrefsMap.getBoolean("security_center_taplus"));
        initHook(new ReduceBrightColorsTile(), mPrefsMap.getBoolean("security_center_reduce_bright_colors_tile"));
        initHook(new FiveGTile(), mPrefsMap.getStringAsInt("system_control_center_5g_new_tile", 0) != 0);
        initHook(new SnowLeopardModeTile(), mPrefsMap.getBoolean("system_ui_control_center_snow_leopard_mode"));
        initHook(new FlashLight(), mPrefsMap.getStringAsInt("security_flash_light_switch", 0) != 0);
        if (mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode_high", 0) != 0) {
            initHook(new SunlightModeHigh());
        } else {
            initHook(new SunlightMode(), mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode", 0) != 0);
        }
        initHook(new QSGridLabels(), mPrefsMap.getInt("system_control_center_old_qs_row", 1) > 1 ||
                mPrefsMap.getBoolean("system_control_center_qs_tile_label"));
        initHook(new MuteVisibleNotifications(), mPrefsMap.getBoolean("system_ui_control_center_mute_visible_notice"));
        initHook(new SwitchCCAndNotification(), mPrefsMap.getBoolean("system_ui_control_center_switch_cc_and_notification"));
        initHook(QSControlDetailBackgroundAlpha.INSTANCE, mPrefsMap.getInt("system_ui_control_center_control_detail_background_alpha", 255) != 255);
        initHook(new MediaControlPanelBackgroundMix(), mPrefsMap.getBoolean("system_ui_control_center_media_control_panel_background_mix"));
        initHook(NotificationWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeatherOld.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeatherNew.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(CompactNotificationsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_compact_notice"));
        /*initHook(CCGridOld.INSTANCE, mPrefsMap.getInt("system_control_center_cc_rows", 4) > 4 ||
                mPrefsMap.getInt("system_control_center_cc_columns", 4) > 4 ||
                (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect") && !isMoreHyperOSVersion(1f)) ||
                mPrefsMap.getBoolean("system_control_center_qs_tile_label"));*/
        initHook(new CCGrid(), (mPrefsMap.getInt("system_control_center_cc_rows", 4) > 4 ||
                mPrefsMap.getInt("system_control_center_cc_columns", 4) > 4 ||
                mPrefsMap.getBoolean("system_ui_control_center_rounded_rect") ||
                mPrefsMap.getBoolean("system_control_center_qs_tile_label")) && !isMoreHyperOSVersion(1f));
        initHook(new QSGrid(), mPrefsMap.getBoolean("system_control_center_old_enable"));
        initHook(new QQSGrid(), mPrefsMap.getBoolean("system_control_center_old_enable"));
        initHook(new AutoCollapse(), mPrefsMap.getBoolean("system_ui_control_auto_close"));
        initHook(RedirectToNotificationChannelSetting.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_redirect_notice"));
        initHook(ControlCenterStyle.INSTANCE, mPrefsMap.getBoolean("system_control_center_unlock_old"));
        initHook(NotificationImportanceHyperOSFix.INSTANCE, mPrefsMap.getBoolean("system_settings_more_notification_settings") && isMoreHyperOSVersion(1f));
        initHook(new NotificationRowMenu(), mPrefsMap.getBoolean("system_ui_control_center_notifrowmenu"));
        initHook(new FixTilesList(), mPrefsMap.getBoolean("system_ui_control_center_fix_tiles_list"));
        initHook(new AllowAllThemesNotificationBlur(), mPrefsMap.getBoolean("system_ui_control_center_unlock_blur_supported"));
        initHook(new DisableTransparent(), mPrefsMap.getBoolean("system_ui_control_center_notification_disable_transparent"));
        initHook(new DisableDeviceManaged(), mPrefsMap.getBoolean("system_ui_control_center_disable_device_managed"));
        initHook(new RemoveNotifNumLimit(), mPrefsMap.getBoolean("system_ui_control_center_remove_notif_num_limit"));

        // Actions
        initHook(new StatusBarActions(), true);

        // Other
        initHook(new UiLockApp(), mPrefsMap.getBoolean("system_framework_guided_access"));
        initHook(new NotificationFix(), mPrefsMap.getBoolean("system_ui_other_notification_fix") && isMoreHyperOSVersion(1f));
        initHook(new BrightnessPct(), mPrefsMap.getBoolean("system_showpct_title"));
        initHook(DisableMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_miui_multi_win_switch"));
        initHook(RemoveMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_remove_miui_multi_win_switch"));
        initHook(DisableInfinitymodeGesture.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_infinitymode_gesture"));
        initHook(DisableBottomBar.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_bottombar"));
        initHook(UnlockClipboard.INSTANCE, mPrefsMap.getBoolean("system_ui_unlock_clipboard"));
        initHook(new VolumeTimerValuesHook(), mPrefsMap.getBoolean("system_ui_volume_timer"));

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
        initHook(new BlockEditor(), mPrefsMap.getBoolean("system_ui_lock_screen_block_editor"));
        initHook(AllowThirdLockScreenUseFace.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_allow_third_face"));
        initHook(new DisableUnlockByBleToast(), mPrefsMap.getBoolean("system_ui_lock_screen_disable_unlock_by_ble_toast"));
        initHook(new LinkageAnimCustomer(), mPrefsMap.getBoolean("system_ui_lock_screen_linkage_anim"));

        initHook(AddBlurEffectToLockScreen.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_blur_button"));
        initHook(AddBlurEffectToNotificationView.INSTANCE, mPrefsMap.getBoolean("n_enable"));
        initHook(BlurButton.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_blur_button"));

        initHook(DoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_double_tap_to_sleep"));

        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));
    }
}
