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
package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.AppLockPinScramble;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.BypassAdbInstallVerify;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.DisableNetworkAssistantOfflineInfoManager;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.DisableReport;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.DisableRootedCheck;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.DisableSafepayAutoScan;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.GetBubbleAppString;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.InstallIntercept;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.IsSbnBelongToActiveBubbleApp;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.NewPrivacyThumbnailBlur;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.RemoveConversationBubbleSettingsRestriction;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.RemoveOpenAppConfirmationPopup;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.SidebarLineCustom;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.UnlockCarSicknessRelief;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.app.AddAppInfoEntry;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.app.AddAppManagerEntry;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.app.AppDefaultSort;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.app.AppDetails;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.app.AppDisable;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.app.AppRestrict;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.app.OpenByDefaultSetting;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.BatteryHealth;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.FastChargeBehavior;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.MoreBatteryInfoNew;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.PowerConsumptionRanking;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.ScreenUsedTime;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.ShowBatteryTemperatureNew;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.UnlockBerserkMode;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.UnlockLowTempExtEndurance;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.UnlockSmartCharge;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery.UnlockSuperWirelessCharge;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.beauty.BeautyLightAuto;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.beauty.BeautyPc;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.beauty.BeautyPrivacy;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.other.BypassSimLockMiAccountAuth;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.other.DisableRootCheck;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.other.LockOneHundredPoints;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.other.NoLowBatteryWarning;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.other.RemoveSIMLockSuccessDialog;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.other.SimplifyMainFragment;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.other.SkipCountDownLimit;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.AddSideBarExpandReceiver;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.BlurSecurity;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.SidebarCommonAppLimit;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.game.GamePerformanceWildMode;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.game.RemoveGameToast;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.game.RemoveMacroBlackList;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.game.UnlockGunService;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.video.DisableRemoveScreenHoldOn;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.video.DisableVideoToolboxThermalClose;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.video.UnlockVideoSomeFunc;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.video.VBVideoMode;
import com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar.video.VideoDolbyOpen;

@HookBase(targetPackage = "com.miui.securitycenter")
public class SecurityCenter extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        // 应用管理
        initHook(new AppDefaultSort(), PrefsBridge.getStringAsInt("security_center_app_default_sort", 0) > 0);
        initHook(AddAppManagerEntry.INSTANCE, PrefsBridge.getBoolean("security_center_aosp_app_manager"));
        initHook(new AppRestrict(), PrefsBridge.getBoolean("security_center_app_restrict"));
        initHook(new AppDisable(), PrefsBridge.getBoolean("security_center_app_disable"));
        initHook(DisableReport.INSTANCE, PrefsBridge.getBoolean("security_center_disable_ban"));
        initHook(new OpenByDefaultSetting(), PrefsBridge.getBoolean("security_center_app_default_setting"));
        initHook(AddAppInfoEntry.INSTANCE, PrefsBridge.getBoolean("security_center_aosp_app_info"));
        initHook(new AppDetails(), PrefsBridge.getBoolean("security_center_app_details"));

        // 省电与电池
        initHook(ScreenUsedTime.INSTANCE, PrefsBridge.getBoolean("security_center_unlock_screen_time"));
        initHook(new PowerConsumptionRanking(), PrefsBridge.getBoolean("security_center_power_consumption_ranking"));
        initHook(new UnlockBerserkMode(), PrefsBridge.getBoolean("security_center_unlock_berserk_mode"));
        initHook(ShowBatteryTemperatureNew.INSTANCE, PrefsBridge.getBoolean("security_center_show_battery_temperature"));
        initHook(BatteryHealth.INSTANCE, PrefsBridge.getBoolean("security_center_show_battery_health"));
        initHook(MoreBatteryInfoNew.INSTANCE, PrefsBridge.getBoolean("secutity_center_battery_show_more_info"));
        initHook(new FastChargeBehavior(), PrefsBridge.getStringAsInt("security_center_fast_charge_notification_mode", 0) != 0);
        initHook(UnlockSuperWirelessCharge.INSTANCE, PrefsBridge.getBoolean("security_center_super_wireless_charge"));
        initHook(new UnlockLowTempExtEndurance(), PrefsBridge.getBoolean("security_center_battery_unlock_low_temp_ext_endurance"));
        initHook(new UnlockSmartCharge(), PrefsBridge.getBoolean("security_center_unlock_smart_charge"));

        // 全局侧边栏
        boolean hideSidebar = PrefsBridge.getBoolean("security_center_hide_sidebar");
        boolean keepSidebarOpen = PrefsBridge.getBoolean("security_center_leave_open");
        boolean isVideoFunc = PrefsBridge.getBoolean("security_center_unlock_memc") ||
                PrefsBridge.getBoolean("security_center_unlock_enhance_contours") ||
                PrefsBridge.getBoolean("security_center_unlock_s_resolution");

        initHook(new AddSideBarExpandReceiver(), hideSidebar || keepSidebarOpen);
        initHook(SidebarLineCustom.INSTANCE, !hideSidebar && PrefsBridge.getBoolean("security_center_sidebar_line_color"));
        initHook(BlurSecurity.INSTANCE, PrefsBridge.getBoolean("se_enable"));
        initHook(new SidebarCommonAppLimit(), PrefsBridge.getBoolean("disable_security_center_sidebar_show_suggest") ||
                PrefsBridge.getBoolean("security_center_sidebar_common_app_limit_enable"));
        initHook(DisableRemoveScreenHoldOn.INSTANCE, PrefsBridge.getBoolean("security_center_disable_remove_screen_hold_on"));
        initHook(new VBVideoMode(), PrefsBridge.getBoolean("security_center_unlock_new_vb"));
        initHook(DisableVideoToolboxThermalClose.INSTANCE, PrefsBridge.getBoolean("security_center_disable_video_toolbox_thermal_close"));
        initHook(new VideoDolbyOpen(), PrefsBridge.getBoolean("security_center_dolby_open"));
        initHook(UnlockVideoSomeFunc.INSTANCE, isVideoFunc);
        initHook(new GamePerformanceWildMode(), PrefsBridge.getBoolean("security_center_game_performance_wild_mode"));
        initHook(new RemoveMacroBlackList(), PrefsBridge.getBoolean("security_center_remove_macro_black_list"));
        initHook(UnlockGunService.INSTANCE, PrefsBridge.getBoolean("security_center_unlock_gun_service"));
        initHook(RemoveGameToast.INSTANCE, PrefsBridge.getBoolean("security_center_remove_game_toast"));

        // 其他
        initHook(BeautyLightAuto.INSTANCE, PrefsBridge.getBoolean("security_center_beauty_light_auto") ||
                PrefsBridge.getBoolean("security_center_beauty_face"));
        initHook(BeautyPrivacy.INSTANCE, PrefsBridge.getBoolean("security_center_beauty_privacy"));
        initHook(BeautyPc.INSTANCE, PrefsBridge.getBoolean("security_center_beauty_pc"));
        initHook(IsSbnBelongToActiveBubbleApp.INSTANCE, PrefsBridge.getBoolean("security_center_unlock_side_hide_freeform"));
        initHook(GetBubbleAppString.INSTANCE, PrefsBridge.getBoolean("security_center_unlock_side_hide_freeform"));
        initHook(new RemoveConversationBubbleSettingsRestriction(), PrefsBridge.getBoolean("security_center_remove_conversation_bubble_settings_restriction"));
        initHook(new DisableRootedCheck(), PrefsBridge.getBoolean("security_center_disable_root_check_environment"));
        initHook(new DisableSafepayAutoScan(), PrefsBridge.getBoolean("security_center_disable_safepay_auto_check"));
        initHook(SimplifyMainFragment.INSTANCE, PrefsBridge.getBoolean("security_center_simplify_home"));
        initHook(new AppLockPinScramble(), PrefsBridge.getBoolean("security_center_applock_pin_scramble"));
        initHook(NewPrivacyThumbnailBlur.INSTANCE, PrefsBridge.getBoolean("security_center_privacy_thumbnail_blur"));
        initHook(LockOneHundredPoints.INSTANCE, PrefsBridge.getBoolean("security_center_score"));
        initHook(new SkipCountDownLimit(), PrefsBridge.getBoolean("security_center_skip_count_down_limit"));
        initHook(DisableRootCheck.INSTANCE, PrefsBridge.getBoolean("security_center_disable_root_check"));
        initHook(new InstallIntercept(), PrefsBridge.getBoolean("security_center_install_intercept"));
        initHook(new BypassAdbInstallVerify(), PrefsBridge.getBoolean("security_center_adb_install_verify"));
        initHook(NoLowBatteryWarning.INSTANCE, PrefsBridge.getBoolean("security_center_remove_low_battery_reminder"));
        initHook(new RemoveOpenAppConfirmationPopup(), PrefsBridge.getBoolean("security_center_remove_open_app_confirmation_popup"));
        initHook(new UnlockCarSicknessRelief(), PrefsBridge.getBoolean("security_center_unlock_car_sickness"));
        initHook(BypassSimLockMiAccountAuth.INSTANCE, PrefsBridge.getBoolean("security_center_bypass_simlock_miaccount_auth"));
        initHook(RemoveSIMLockSuccessDialog.INSTANCE, PrefsBridge.getBoolean("security_center_remove_simlock_success_dialog"));
        initHook(new DisableNetworkAssistantOfflineInfoManager(), PrefsBridge.getBoolean("security_center_disable_offline_info_manager"));

    }
}
