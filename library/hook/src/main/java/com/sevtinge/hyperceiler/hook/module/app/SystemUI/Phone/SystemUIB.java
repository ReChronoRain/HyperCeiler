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
package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.systemframework.volume.VolumeMediaSteps;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.AutoCollapse;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.AutoSEffSwitchForSystemUi;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.UnlockClipboard;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.MiuiStub;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.AutoDismissExpandedPopupsHook;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.ExpandNotificationKt;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.FiveGTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.FixTilesList;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.GmsTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NewFlashLight;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationImportanceHyperOSFix;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.OldWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.ReduceBrightColorsTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.SnowLeopardModeTile;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.SunlightMode;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.SunlightModeHigh;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.UnimportantNotification;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.CustomBackground;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewLayout;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewSize;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.UnlockCustomActions;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.b.MediaPicture;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.b.MediaSeekBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.HideLockScreenHint;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.HideLockscreenZenMode;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.LockScreenDoubleTapToSleep;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.ScramblePIN;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.navigation.RotationButtonB;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.BrightnessPct;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.DisableBottomBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.DisableMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.MonetThemeOverlay;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.NotificationFreeform;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.other.RemoveMiuiMultiWinSwitch;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.NewPluginHelperKt;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.systemui.QSColor;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.DoubleTapToSleep;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.NotificationIconColumns;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.clock.StatusBarClockNew;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.SwapWiFiAndMobileNetwork;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v.FocusNotifLyric;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v.HideFakeStatusBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.model.MobileTypeSingle2Hook;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.model.MobileTypeTextCustom;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeed;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeedStyle;
import com.sevtinge.hyperceiler.hook.module.skip.StatusBarActions;

import java.util.Objects;

@HookBase(targetPackage = "com.android.systemui", isPad = 2, targetSdk = 36)
public class SystemUIB extends BaseModule {

    @Override
    public void handleLoadPackage() {
        MiuiStub.createHook();
        // PluginHelper
        initHook(NewPluginHelperKt.INSTANCE);
        // Actions
        initHook(new StatusBarActions(), true);

        // 锁屏
        initHook(HideLockScreenHint.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_unlock_tip"));
        initHook(HideLockscreenZenMode.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_not_disturb_mode"));
        initHook(new ScramblePIN(), mPrefsMap.getBoolean("system_ui_lock_screen_scramble_pin"));
        initHook(LockScreenDoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_lock_screen_double_lock"));

        // 状态栏图标
        initHook(SwapWiFiAndMobileNetwork.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_swap_wifi_and_mobile_network"));
        initHook(new NotificationIconColumns(), mPrefsMap.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(NotificationImportanceHyperOSFix.INSTANCE, mPrefsMap.getBoolean("system_settings_more_notification_settings"));

        // 移动网络图标
        // boolean isEnabledDualRowSignal = mPrefsMap.getBoolean("system_ui_statusbar_network_icon_enable");
        // initHook(new DualRowSignalHookV(), isEnabledDualRowSignal);
        // initHook(new MobilePublicHookV(), isEnabledDualRowSignal ||
        //     mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_1") ||
        //     mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_2") ||
        //     mPrefsMap.getBoolean("system_ui_status_bar_mobile_hide_roaming_icon") ||
        //     mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator") ||
        //     mPrefsMap.getStringAsInt("system_ui_status_bar_icon_small_hd", 0) != 0 ||
        //     mPrefsMap.getStringAsInt("system_ui_status_bar_icon_big_hd", 0) != 0);
        initHook(MobileTypeSingle2Hook.INSTANCE, mPrefsMap.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0) != 0 ||
            mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable"));
        initHook(MobileTypeTextCustom.INSTANCE, !Objects.equals(mPrefsMap.getString("system_ui_status_bar_mobile_type_custom", ""), ""));

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
        if (mPrefsMap.getBoolean("system_ui_statusbar_music_switch") && isHyperOSVersion(2f)) {
            initHook(FocusNotifLyric.INSTANCE);
            initHook(HideFakeStatusBar.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_music_hide_clock"));
        }

        // 导航栏
        initHook(RotationButtonB.INSTANCE, mPrefsMap.getStringAsInt("system_framework_other_rotation_button_int", 0) != 0);

        // 控制与通知中心
        initHook(new QSColor(), mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") || mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color"));
        initHook(OldWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(AutoDismissExpandedPopupsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_auto_clean_expand_notification"));
        initHook(ExpandNotificationKt.INSTANCE, !mPrefsMap.getStringSet("system_ui_control_center_expand_notification").isEmpty());
        initHook(new UnimportantNotification(), mPrefsMap.getBoolean("system_ui_control_center_unimportant_notification"));

        // 磁贴
        initHook(new AutoCollapse(), mPrefsMap.getBoolean("system_ui_control_auto_close"));
        initHook(new SnowLeopardModeTile(), mPrefsMap.getBoolean("system_ui_control_center_snow_leopard_mode"));
        initHook(new GmsTile(), mPrefsMap.getBoolean("security_center_gms_open"));
        // initHook(new TaplusTile(), mPrefsMap.getBoolean("security_center_taplus"));
        initHook(new ReduceBrightColorsTile(), mPrefsMap.getBoolean("security_center_reduce_bright_colors_tile"));
        initHook(new FiveGTile(), mPrefsMap.getStringAsInt("system_control_center_5g_new_tile", 0) != 0);
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
            || mPrefsMap.getInt("system_ui_control_center_media_control_seekbar_thumb_color", -1) != -1 ||
            mPrefsMap.getStringAsInt("system_ui_control_center_media_control_progress_mode", 0) != 0);

        // Other
        initHook(DoubleTapToSleep.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_double_tap_to_sleep"));

        initHook(new MonetThemeOverlay(), mPrefsMap.getBoolean("system_ui_monet_overlay_custom"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));
        initHook(new NotificationFreeform(), mPrefsMap.getBoolean("system_ui_notification_freeform"));
        initHook(new BrightnessPct(), mPrefsMap.getBoolean("system_showpct_title"));
        initHook(DisableMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_miui_multi_win_switch"));
        initHook(RemoveMiuiMultiWinSwitch.INSTANCE, mPrefsMap.getBoolean("system_ui_remove_miui_multi_win_switch"));
        initHook(DisableBottomBar.INSTANCE, mPrefsMap.getBoolean("system_ui_disable_bottombar"));
        initHook(UnlockClipboard.INSTANCE, mPrefsMap.getBoolean("system_ui_unlock_clipboard"));

        initHook(new VolumeMediaSteps(), mPrefsMap.getBoolean("system_framework_volume_media_steps_enable"));
        if (mPrefsMap.getBoolean("misound_bluetooth") && isHyperOSVersion(2f)) {
            initHook(new AutoSEffSwitchForSystemUi().onApplication());
        }
    }
}
