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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.app.SecurityCenter.Pad;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.securitycenter.AppLockPinScramble;
import com.sevtinge.hyperceiler.module.hook.securitycenter.BypassAdbInstallVerify;
import com.sevtinge.hyperceiler.module.hook.securitycenter.DisableReport;
import com.sevtinge.hyperceiler.module.hook.securitycenter.GetBubbleAppString;
import com.sevtinge.hyperceiler.module.hook.securitycenter.HideXOptModeTip;
import com.sevtinge.hyperceiler.module.hook.securitycenter.InstallIntercept;
import com.sevtinge.hyperceiler.module.hook.securitycenter.IsSbnBelongToActiveBubbleApp;
import com.sevtinge.hyperceiler.module.hook.securitycenter.NewBoxBlur;
import com.sevtinge.hyperceiler.module.hook.securitycenter.PowerSaver;
import com.sevtinge.hyperceiler.module.hook.securitycenter.PrivacyThumbnailBlur;
import com.sevtinge.hyperceiler.module.hook.securitycenter.RemoveConversationBubbleSettingsRestriction;
import com.sevtinge.hyperceiler.module.hook.securitycenter.RemoveOpenAppConfirmationPopup;
import com.sevtinge.hyperceiler.module.hook.securitycenter.ScLockApp;
import com.sevtinge.hyperceiler.module.hook.securitycenter.SidebarLineCustom;
import com.sevtinge.hyperceiler.module.hook.securitycenter.UnlockFbo;
import com.sevtinge.hyperceiler.module.hook.securitycenter.app.AddAppInfoEntry;
import com.sevtinge.hyperceiler.module.hook.securitycenter.app.AddAppManagerEntry;
import com.sevtinge.hyperceiler.module.hook.securitycenter.app.AppDefaultSort;
import com.sevtinge.hyperceiler.module.hook.securitycenter.app.AppDetails;
import com.sevtinge.hyperceiler.module.hook.securitycenter.app.AppDisable;
import com.sevtinge.hyperceiler.module.hook.securitycenter.app.AppRestrict;
import com.sevtinge.hyperceiler.module.hook.securitycenter.app.OpenByDefaultSetting;
import com.sevtinge.hyperceiler.module.hook.securitycenter.battery.BatteryHealth;
import com.sevtinge.hyperceiler.module.hook.securitycenter.battery.PowerConsumptionRanking;
import com.sevtinge.hyperceiler.module.hook.securitycenter.battery.ScreenUsedTime;
import com.sevtinge.hyperceiler.module.hook.securitycenter.battery.ShowBatteryTemperatureNew;
import com.sevtinge.hyperceiler.module.hook.securitycenter.battery.UnlockLowTempExtEndurance;
import com.sevtinge.hyperceiler.module.hook.securitycenter.battery.UnlockSmartCharge;
import com.sevtinge.hyperceiler.module.hook.securitycenter.battery.UnlockSuperWirelessCharge;
import com.sevtinge.hyperceiler.module.hook.securitycenter.beauty.BeautyLightAuto;
import com.sevtinge.hyperceiler.module.hook.securitycenter.beauty.BeautyPc;
import com.sevtinge.hyperceiler.module.hook.securitycenter.beauty.BeautyPrivacy;
import com.sevtinge.hyperceiler.module.hook.securitycenter.lab.AiClipboardEnable;
import com.sevtinge.hyperceiler.module.hook.securitycenter.lab.BlurLocationEnable;
import com.sevtinge.hyperceiler.module.hook.securitycenter.lab.GetNumberEnable;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.BypassSimLockMiAccountAuth;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.DisableRootCheck;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.FuckRiskPkg;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.LockOneHundredPoints;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.NoLowBatteryWarning;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.RemoveSIMLockSuccessDialog;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.SimplifyMainFragment;
import com.sevtinge.hyperceiler.module.hook.securitycenter.other.SkipCountDownLimit;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.AddSideBarExpandReceiver;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.BlurSecurity;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game.GamePerformanceWildMode;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game.RemoveMacroBlackList;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game.UnlockGunService;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video.DisableRemoveScreenHoldOn;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video.UnlockVideoSomeFunc;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video.VBVideoMode;
import com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.video.VideoDolbyOpen;

@HookBase(targetPackage = "com.miui.securitycenter", isPad = true, targetSdk = 33)
public class SecurityCenterT extends BaseModule {

    @Override
    public void handleLoadPackage() {

        // 应用管理
        initHook(new AppDefaultSort(), mPrefsMap.getStringAsInt("security_center_app_default_sort", 0) > 0);
        initHook(new AppRestrict(), mPrefsMap.getBoolean("security_center_app_restrict"));
        initHook(new AppDisable(), mPrefsMap.getBoolean("security_center_app_disable"));
        initHook(new AppDetails(), mPrefsMap.getBoolean("security_center_app_details"));
        initHook(DisableReport.INSTANCE, mPrefsMap.getBoolean("security_center_disable_ban"));
        initHook(new OpenByDefaultSetting(), mPrefsMap.getBoolean("security_center_app_default_setting"));
        initHook(AddAppInfoEntry.INSTANCE, mPrefsMap.getBoolean("security_center_aosp_app_info"));
        initHook(AddAppManagerEntry.INSTANCE, mPrefsMap.getBoolean("security_center_aosp_app_manager"));

        // 省电与电池
        initHook(ShowBatteryTemperatureNew.INSTANCE, mPrefsMap.getBoolean("security_center_show_battery_temperature"));
        initHook(UnlockSuperWirelessCharge.INSTANCE, mPrefsMap.getBoolean("security_center_super_wireless_charge"));
        initHook(ScreenUsedTime.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_screen_time"));
        initHook(new UnlockSmartCharge(), mPrefsMap.getBoolean("security_center_unlock_smart_charge"));
        initHook(BatteryHealth.INSTANCE, mPrefsMap.getBoolean("security_center_show_battery_health"));
        initHook(new UnlockLowTempExtEndurance(), mPrefsMap.getBoolean("security_center_battery_unlock_low_temp_ext_endurance"));

        // 隐私保护
        initHook(new AppLockPinScramble(), mPrefsMap.getBoolean("security_center_applock_pin_scramble"));
        initHook(AiClipboardEnable.INSTANCE, mPrefsMap.getBoolean("security_center_ai_clipboard"));
        initHook(BlurLocationEnable.INSTANCE, mPrefsMap.getBoolean("security_center_blur_location"));
        initHook(GetNumberEnable.INSTANCE, mPrefsMap.getBoolean("security_center_get_number"));
        initHook(new HideXOptModeTip(), mPrefsMap.getBoolean("security_center_hide_xopt_mode_tip"));

        // 前置摄像助手
        initHook(BeautyLightAuto.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_face") ||
                mPrefsMap.getBoolean("security_center_beauty_light_auto"));
        initHook(BeautyPrivacy.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_privacy"));
        initHook(BeautyPc.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_pc"));

        // 其他
        initHook(SimplifyMainFragment.INSTANCE, mPrefsMap.getBoolean("security_center_simplify_home"));
        initHook(new InstallIntercept(), mPrefsMap.getBoolean("security_center_install_intercept"));
        initHook(LockOneHundredPoints.INSTANCE, mPrefsMap.getBoolean("security_center_score"));
        initHook(new SkipCountDownLimit(), mPrefsMap.getBoolean("security_center_skip_count_down_limit"));
        initHook(DisableRootCheck.INSTANCE, mPrefsMap.getBoolean("security_center_disable_root_check"));
        initHook(FuckRiskPkg.INSTANCE, mPrefsMap.getBoolean("security_center_disable_send_malicious_app_notification"));
        initHook(NoLowBatteryWarning.INSTANCE, mPrefsMap.getBoolean("security_center_remove_low_battery_reminder"));
        initHook(RemoveSIMLockSuccessDialog.INSTANCE, mPrefsMap.getBoolean("security_center_remove_simlock_success_dialog"));
        initHook(new UnlockFbo(), mPrefsMap.getBoolean("security_center_unlock_fbo"));
        initHook(BypassSimLockMiAccountAuth.INSTANCE, mPrefsMap.getBoolean("security_center_bypass_simlock_miaccount_auth"));
        initHook(new BypassAdbInstallVerify(), mPrefsMap.getBoolean("security_center_adb_install_verify"));

        // 小窗和气泡通知
        initHook(new RemoveConversationBubbleSettingsRestriction(), mPrefsMap.getBoolean("security_center_remove_conversation_bubble_settings_restriction"));
        initHook(IsSbnBelongToActiveBubbleApp.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_side_hide_freeform"));
        initHook(GetBubbleAppString.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_side_hide_freeform"));

        // 移除打开应用弹窗
        initHook(new RemoveOpenAppConfirmationPopup(), mPrefsMap.getBoolean("security_center_remove_open_app_confirmation_popup"));

        // 全局侧边栏
        boolean isVideoFunc = mPrefsMap.getBoolean("security_center_unlock_memc") ||
                mPrefsMap.getBoolean("security_center_unlock_s_resolution") ||
                mPrefsMap.getBoolean("security_center_unlock_enhance_contours");

        initHook(new PrivacyThumbnailBlur(), mPrefsMap.getBoolean("security_center_privacy_thumbnail_blur"));
        initHook(new PowerSaver(), mPrefsMap.getBoolean("security_center_power_saver"));
        initHook(new NewBoxBlur(), mPrefsMap.getBoolean("security_center_newbox_custom_enable"));
        initHook(BlurSecurity.INSTANCE, mPrefsMap.getBoolean("se_enable"));
        initHook(SidebarLineCustom.INSTANCE, mPrefsMap.getBoolean("security_center_sidebar_line_color"));
        initHook(new ScLockApp(), mPrefsMap.getBoolean("system_framework_guided_access_sc"));
        initHook(new RemoveMacroBlackList(), mPrefsMap.getBoolean("security_center_remove_macro_black_list"));
        initHook(UnlockGunService.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_gun_service"));
        initHook(DisableRemoveScreenHoldOn.INSTANCE, mPrefsMap.getBoolean("security_center_disable_remove_screen_hold_on"));
        initHook(UnlockVideoSomeFunc.INSTANCE, isVideoFunc);
        initHook(new AddSideBarExpandReceiver(), mPrefsMap.getBoolean("security_center_hide_sidebar"));
        // initHook(new DockSuggest(), mPrefsMap.getStringAsInt("security_center_sidebar_show_suggest", 0) != 0);
        initHook(new VideoDolbyOpen(), mPrefsMap.getBoolean("security_center_dolby_open"));
        initHook(new VBVideoMode(), mPrefsMap.getBoolean("security_center_unlock_new_vb"));
        initHook(new GamePerformanceWildMode(), mPrefsMap.getBoolean("security_center_game_performance_wild_mode"));

        initHook(new PowerConsumptionRanking(), mPrefsMap.getBoolean("security_center_power_consumption_ranking"));

        // initHook(new EnableGameSpeed(), mPrefsMap.getBoolean("security_center_game_speed"));

        // reshook
        initHook(SidebarLineCustom.INSTANCE, mPrefsMap.getBoolean("security_center_sidebar_line_color"));

    }
}
