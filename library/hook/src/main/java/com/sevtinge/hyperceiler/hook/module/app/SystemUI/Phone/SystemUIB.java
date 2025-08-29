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
import com.sevtinge.hyperceiler.hook.module.hook.systemui.AutoSEffSwitchForSystemUi;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.MiuiStub;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.AutoDismissExpandedPopupsHook;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.ExpandNotificationKt;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationImportanceHyperOSFix;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.OldWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.UnimportantNotification;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.CustomBackground;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewLayout;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewSize;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.UnlockCustomActions;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.b.MediaPicture;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.b.MediaSeekBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.NewPluginHelperKt;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.systemui.QSColor;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.NotificationIconColumns;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.clock.StatusBarClockNew;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.IconsFromSystemManager;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.StatusBarIcon;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.SwapWiFiAndMobileNetwork;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.all.WifiNetworkIndicator;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v.FocusNotifLyric;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v.HideFakeStatusBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.icon.v.WifiStandard;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.model.MobilePublicHookV;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.model.MobileTypeSingle2Hook;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.model.MobileTypeTextCustom;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeed;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeedStyle;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.strongtoast.HideStrongToast;

import java.util.Objects;

@HookBase(targetPackage = "com.android.systemui", isPad = 2, targetSdk = 36)
public class SystemUIB extends BaseModule {

    @Override
    public void handleLoadPackage() {
        MiuiStub.createHook();
        // PluginHelper
        initHook(NewPluginHelperKt.INSTANCE);

        // 状态栏图标
        initHook(new StatusBarIcon(), true);
        initHook(new IconsFromSystemManager(), true);
        initHook(new NotificationIconColumns(), mPrefsMap.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(NotificationImportanceHyperOSFix.INSTANCE, mPrefsMap.getBoolean("system_settings_more_notification_settings"));
        initHook(SwapWiFiAndMobileNetwork.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_swap_wifi_and_mobile_network"));
        initHook(WifiNetworkIndicator.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_icon_wifi_network_indicator_new"));
        initHook(WifiStandard.INSTANCE, mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_standard", 0) > 0);

        // 移动网络图标
        boolean isEnabledDualRowSignal = mPrefsMap.getBoolean("system_ui_statusbar_network_icon_enable");
        // initHook(new DualRowSignalHookV(), isEnabledDualRowSignal);
        initHook(new MobilePublicHookV(), isEnabledDualRowSignal ||
            mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_1") ||
            mPrefsMap.getBoolean("system_ui_status_bar_icon_mobile_network_hide_card_2") ||
            mPrefsMap.getBoolean("system_ui_status_bar_mobile_hide_roaming_icon") ||
            mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator") ||
            mPrefsMap.getStringAsInt("system_ui_status_bar_icon_small_hd", 0) != 0 ||
            mPrefsMap.getStringAsInt("system_ui_status_bar_icon_big_hd", 0) != 0);
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

        // 灵动舞台（使用 prop 关闭灵动岛后会回退到此，因此在 HyperOS 3 上依然需要此 hook）
        initHook(HideStrongToast.INSTANCE, mPrefsMap.getBoolean("system_ui_status_bar_hide_smart_strong_toast"));

        // 控制与通知中心
        initHook(new QSColor(), mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") || mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color"));
        initHook(OldWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(AutoDismissExpandedPopupsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_auto_clean_expand_notification"));
        initHook(ExpandNotificationKt.INSTANCE, !mPrefsMap.getStringSet("system_ui_control_center_expand_notification").isEmpty());
        initHook(new UnimportantNotification(), mPrefsMap.getBoolean("system_ui_control_center_unimportant_notification"));

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

        if (mPrefsMap.getBoolean("misound_bluetooth") && isHyperOSVersion(2f)) {
            initHook(new AutoSEffSwitchForSystemUi().onApplication());
        }
    }
}
