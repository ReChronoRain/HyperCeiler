package com.sevtinge.cemiuiler.module.app;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.base.CloseHostDir;
import com.sevtinge.cemiuiler.module.base.LoadHostDir;
import com.sevtinge.cemiuiler.module.hook.securitycenter.AppLockPinScramble;
import com.sevtinge.cemiuiler.module.hook.securitycenter.BlurSecurity;
import com.sevtinge.cemiuiler.module.hook.securitycenter.DisableReport;
import com.sevtinge.cemiuiler.module.hook.securitycenter.GetBubbleAppString;
import com.sevtinge.cemiuiler.module.hook.securitycenter.IsSbnBelongToActiveBubbleApp;
import com.sevtinge.cemiuiler.module.hook.securitycenter.NewBoxBlur;
import com.sevtinge.cemiuiler.module.hook.securitycenter.RemoveConversationBubbleSettingsRestriction;
import com.sevtinge.cemiuiler.module.hook.securitycenter.RemoveOpenAppConfirmationPopup;
import com.sevtinge.cemiuiler.module.hook.securitycenter.ScreenUsedTime;
import com.sevtinge.cemiuiler.module.hook.securitycenter.ShowBatteryTemperatureNew;
import com.sevtinge.cemiuiler.module.hook.securitycenter.SidebarLineCustom;
import com.sevtinge.cemiuiler.module.hook.securitycenter.UnlockSuperWirelessCharge;
import com.sevtinge.cemiuiler.module.hook.securitycenter.VideoDolbyOpen;
import com.sevtinge.cemiuiler.module.hook.securitycenter.app.AppDefaultSort;
import com.sevtinge.cemiuiler.module.hook.securitycenter.app.AppDetails;
import com.sevtinge.cemiuiler.module.hook.securitycenter.app.AppDisable;
import com.sevtinge.cemiuiler.module.hook.securitycenter.app.AppRestrict;
import com.sevtinge.cemiuiler.module.hook.securitycenter.app.OpenByDefaultSetting;
import com.sevtinge.cemiuiler.module.hook.securitycenter.beauty.BeautyFace;
import com.sevtinge.cemiuiler.module.hook.securitycenter.beauty.BeautyLightAuto;
import com.sevtinge.cemiuiler.module.hook.securitycenter.beauty.BeautyPc;
import com.sevtinge.cemiuiler.module.hook.securitycenter.beauty.BeautyPrivacy;
import com.sevtinge.cemiuiler.module.hook.securitycenter.lab.AiClipboardEnable;
import com.sevtinge.cemiuiler.module.hook.securitycenter.lab.BlurLocationEnable;
import com.sevtinge.cemiuiler.module.hook.securitycenter.lab.GetNumberEnable;
import com.sevtinge.cemiuiler.module.hook.securitycenter.other.DisableRootCheck;
import com.sevtinge.cemiuiler.module.hook.securitycenter.other.FuckRiskPkg;
import com.sevtinge.cemiuiler.module.hook.securitycenter.other.LockOneHundredPoints;
import com.sevtinge.cemiuiler.module.hook.securitycenter.other.NoLowBatteryWarning;
import com.sevtinge.cemiuiler.module.hook.securitycenter.other.SkipCountDownLimit;
import com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.AddSideBarExpandReceiver;
import com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.game.RemoveMacroBlackList;
import com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.game.UnlockGunService;
import com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.video.DisableRemoveScreenHoldOn;
import com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.video.UnlockEnhanceContours;
import com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.video.UnlockMemc;
import com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar.video.UnlockSuperResolution;

public class SecurityCenter extends BaseModule {

    @Override
    public void handleLoadPackage() {

        // dexKit load
        initHook(LoadHostDir.INSTANCE);

        // 应用管理
        initHook(new AppDefaultSort(), mPrefsMap.getStringAsInt("security_center_app_default_sort", 0) > 0);
        initHook(new AppRestrict(), mPrefsMap.getBoolean("security_center_app_restrict"));
        initHook(new AppDisable(), mPrefsMap.getBoolean("security_center_app_disable"));
        initHook(new AppDetails(), mPrefsMap.getBoolean("security_center_app_details"));
        initHook(DisableReport.INSTANCE, mPrefsMap.getBoolean("security_center_disable_ban"));
        initHook(OpenByDefaultSetting.INSTANCE, mPrefsMap.getBoolean("security_center_app_default_setting"));

        // 省电与电池
        // initHook(new ShowBatteryTemperature(), mPrefsMap.getBoolean("security_center_show_battery_temperature"));
        initHook(ShowBatteryTemperatureNew.INSTANCE, mPrefsMap.getBoolean("security_center_show_battery_temperature"));
        initHook(UnlockSuperWirelessCharge.INSTANCE, mPrefsMap.getBoolean("security_center_super_wireless_charge"));
        initHook(ScreenUsedTime.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_screen_time"));

        // 隐私保护
        initHook(new AppLockPinScramble(), mPrefsMap.getBoolean("security_center_applock_pin_scramble"));
        initHook(AiClipboardEnable.INSTANCE, mPrefsMap.getBoolean("security_center_ai_clipboard"));
        initHook(BlurLocationEnable.INSTANCE, mPrefsMap.getBoolean("security_center_blur_location"));
        initHook(GetNumberEnable.INSTANCE, mPrefsMap.getBoolean("security_center_get_number"));

        // 前置摄像助手
        initHook(BeautyLightAuto.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_light_auto"));
        initHook(BeautyFace.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_face"));
        initHook(BeautyPrivacy.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_privacy"));
        initHook(BeautyPc.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_pc"));

        // 其他
        initHook(LockOneHundredPoints.INSTANCE, mPrefsMap.getBoolean("security_center_score"));
        initHook(new SkipCountDownLimit(), mPrefsMap.getBoolean("security_center_skip_count_down_limit"));
        initHook(DisableRootCheck.INSTANCE, mPrefsMap.getBoolean("security_center_disable_root_check"));
        initHook(FuckRiskPkg.INSTANCE, mPrefsMap.getBoolean("security_center_disable_send_malicious_app_notification"));
        initHook(NoLowBatteryWarning.INSTANCE, mPrefsMap.getBoolean("security_center_remove_low_battery_reminder"));

        // 小窗和气泡通知
        initHook(new RemoveConversationBubbleSettingsRestriction(), mPrefsMap.getBoolean("security_center_remove_conversation_bubble_settings_restriction"));
        initHook(IsSbnBelongToActiveBubbleApp.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_side_hide_freeform"));
        initHook(GetBubbleAppString.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_side_hide_freeform"));

        // 移除打开应用弹窗
        initHook(new RemoveOpenAppConfirmationPopup(), mPrefsMap.getBoolean("security_center_remove_open_app_confirmation_popup"));

        // 全局侧边栏
        if (!isAndroidR()) {
            initHook(new NewBoxBlur(), mPrefsMap.getBoolean("security_center_newbox_custom_enable"));
            initHook(BlurSecurity.INSTANCE, mPrefsMap.getBoolean("se_enable"));
            initHook(SidebarLineCustom.INSTANCE, mPrefsMap.getBoolean("security_center_sidebar_line_color"));
        }
        initHook(new RemoveMacroBlackList(), mPrefsMap.getBoolean("security_center_remove_macro_black_list"));
        initHook(UnlockGunService.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_gun_service"));
        initHook(DisableRemoveScreenHoldOn.INSTANCE, mPrefsMap.getBoolean("security_center_disable_remove_screen_hold_on"));
        initHook(UnlockMemc.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_memc"));
        initHook(UnlockSuperResolution.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_s_resolution"));
        initHook(UnlockEnhanceContours.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_enhance_contours"));
        initHook(new AddSideBarExpandReceiver(), mPrefsMap.getBoolean("security_center_hide_sidebar"));
        // initHook(new DisableDockSuggest(), mPrefsMap.getBoolean("security_center_disable_sidebar_show_suggest"));
        initHook(new VideoDolbyOpen(), mPrefsMap.getBoolean("security_center_dolby_open"));

        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
