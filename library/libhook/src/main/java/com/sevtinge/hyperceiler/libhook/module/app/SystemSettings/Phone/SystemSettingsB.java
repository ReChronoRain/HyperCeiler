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
package com.sevtinge.hyperceiler.hook.module.app.SystemSettings.Phone;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.AddGoogleListHeader;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.AntiQues;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.AppsFreezerEnable;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.ControlCenterStyle;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.EnableSpeedMode;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.HyperCeilerSettings;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.InternationalBuild;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.LinkTurbo;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.ModifySystemVersion;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.MoreNotificationSettings;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.MoreVpnTypes;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.NewNFCPage;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.NoveltyHaptic;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.QuickManageOverlayPermission;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.QuickManagerAccessibilityPermission;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.RunningServices;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.UnlockMaxFps;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.UnlockNeverSleepScreen;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.UnlockTaplusForSettings;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.UnlockXiaomiHyperAIEntranceKt;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.UsbModeChoose;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.ViewWifiPasswordHook;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.aiimage.UnlockAi;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.aiimage.UnlockMemc;
import com.sevtinge.hyperceiler.hook.module.rules.systemsettings.aiimage.UnlockSuperResolution;

@HookBase(targetPackage = "com.android.settings", isPad = 2, targetSdk = 36)
public class SystemSettingsB extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // 首页
        initHook(new HyperCeilerSettings(), mPrefsMap.getStringAsInt("settings_icon", 0) != 0);
        initHook(new InternationalBuild(), mPrefsMap.getBoolean("system_settings_international_build"));
        initHook(UnlockXiaomiHyperAIEntranceKt.INSTANCE, mPrefsMap.getBoolean("system_settings_unlock_xiaomihyperai_entrance"));
        initHook(new AddGoogleListHeader(), mPrefsMap.getBoolean("system_settings_unlock_google_header"));

        // 系统更新伪装版本
        initHook(new ModifySystemVersion(), mPrefsMap.getBoolean("updater_enable_miui_version") && mPrefsMap.getStringAsInt("updater_version_mode", 1) != 1);

        // VPN / 网络连接与共享
        initHook(new ViewWifiPasswordHook(), mPrefsMap.getBoolean("system_settings_safe_wifi"));
        initHook(new LinkTurbo(), mPrefsMap.getBoolean("system_settings_linkturbo"));
        initHook(new NewNFCPage(), mPrefsMap.getBoolean("system_settings_new_nfc_page"));
        initHook(new MoreVpnTypes(), mPrefsMap.getBoolean("system_settings_more_vpn_types"));

        // 特色功能
        initHook(UnlockTaplusForSettings.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));

        // 显示与息屏
        initHook(new UnlockNeverSleepScreen(), mPrefsMap.getBoolean("system_settings_allow_never_lock_screen"));
        initHook(new UnlockSuperResolution(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_sr"));
        initHook(new UnlockAi(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_ai"));
        initHook(new UnlockMemc(), mPrefsMap.getBoolean("system_settings_ai_image_unlock_memc"));

        // 权限
        initHook(new MoreNotificationSettings(), mPrefsMap.getBoolean("system_settings_more_notification_settings"));
        initHook(new QuickManageOverlayPermission(), mPrefsMap.getBoolean("system_settings_permission_show_app_up"));
        initHook(new QuickManagerAccessibilityPermission(), mPrefsMap.getBoolean("system_settings_permission_accessibility"));

        // 开发者选项
        initHook(new UsbModeChoose(), mPrefsMap.getStringAsInt("system_settings_usb_mode_choose", 0) != 0
            || mPrefsMap.getBoolean("system_settings_usb_mode"));
        initHook(new AppsFreezerEnable(), mPrefsMap.getBoolean("system_settings_apps_freezer"));
        initHook(UnlockMaxFps.INSTANCE, mPrefsMap.getBoolean("system_settings_develop_max_fps"));
        initHook(new EnableSpeedMode(), mPrefsMap.getBoolean("system_settings_develop_speed_mode"));

        // Others
        initHook(new AntiQues(), mPrefsMap.getBoolean("system_settings_anti_ques"));
        initHook(new AllowManageAllNotifications(), mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"));
        initHook(new RunningServices(), true); // 显示原生内存信息
        // initHook(new BluetoothRestrict(), mPrefsMap.getBoolean("various_disable_bluetooth_restrict"));
        initHook(new ControlCenterStyle(), mPrefsMap.getBoolean("system_control_center_unlock_old"));
        initHook(NoveltyHaptic.INSTANCE, mPrefsMap.getBoolean("system_settings_novelty_haptic"));
        initHook(UnlockTaplusForSettings.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));
    }
}
