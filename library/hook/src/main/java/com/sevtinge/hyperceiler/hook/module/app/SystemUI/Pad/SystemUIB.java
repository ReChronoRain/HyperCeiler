package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Pad;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.MediaControlBgFactory;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.AutoDismissExpandedPopupsHook;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationImportanceHyperOSFix;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.NotificationWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.OldWeather;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.CustomBackground;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewLayout;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.MediaViewSize;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.UnlockCustomActions;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.b.MediaPicture;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.b.MediaSeekBar;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.NotificationIconColumns;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.clock.StatusBarClockNew;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSec;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NetworkSpeedSpacing;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeed;
import com.sevtinge.hyperceiler.hook.module.hook.systemui.statusbar.network.NewNetworkSpeedStyle;

@HookBase(targetPackage = "com.android.systemui", isPad = 1, targetSdk = 36)
public class SystemUIB extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // 状态栏图标
        initHook(new NotificationIconColumns(), mPrefsMap.getBoolean("system_ui_status_bar_notification_icon_maximum_enable"));
        initHook(NotificationImportanceHyperOSFix.INSTANCE, mPrefsMap.getBoolean("system_settings_more_notification_settings"));

        // 网速指示器
        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_all_status_enable")) {
            initHook(NewNetworkSpeed.INSTANCE, true);
            initHook(NewNetworkSpeedStyle.INSTANCE, true);
            initHook(new NetworkSpeedSpacing(), mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing", 3) != 3);
            initHook(new NetworkSpeedSec(), mPrefsMap.getBoolean("system_ui_statusbar_network_speed_sec_unit"));
        }

        // 时钟指示器
        initHook(StatusBarClockNew.INSTANCE, mPrefsMap.getBoolean("system_ui_statusbar_clock_all_status_enable"));

        // 控制与通知中心
        initHook(OldWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(NotificationWeather.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_show_weather"));
        initHook(AutoDismissExpandedPopupsHook.INSTANCE, mPrefsMap.getBoolean("system_ui_control_center_auto_clean_expand_notification"));

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

    }
}
