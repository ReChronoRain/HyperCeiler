package com.sevtinge.cemiuiler.module;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isAndroidR;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.securitycenter.AppLockPinScramble;
import com.sevtinge.cemiuiler.module.securitycenter.BlurSecurity;
import com.sevtinge.cemiuiler.module.securitycenter.DisableReport;
import com.sevtinge.cemiuiler.module.securitycenter.DisableRootCheck;
import com.sevtinge.cemiuiler.module.securitycenter.FuckRiskPkg;
import com.sevtinge.cemiuiler.module.securitycenter.GetBubbleAppString;
import com.sevtinge.cemiuiler.module.securitycenter.IsSbnBelongToActiveBubbleApp;
import com.sevtinge.cemiuiler.module.securitycenter.LockOneHundredPoints;
import com.sevtinge.cemiuiler.module.securitycenter.NewBoxBlur;
import com.sevtinge.cemiuiler.module.securitycenter.RemoveConversationBubbleSettingsRestriction;
import com.sevtinge.cemiuiler.module.securitycenter.ScreenUsedTime;
import com.sevtinge.cemiuiler.module.securitycenter.sidebar.game.RemoveMacroBlackList;
import com.sevtinge.cemiuiler.module.securitycenter.RemoveOpenAppConfirmationPopup;
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit;
import com.sevtinge.cemiuiler.module.securitycenter.ShowBatteryTemperatureNew;
import com.sevtinge.cemiuiler.module.securitycenter.SidebarLineCustom;
import com.sevtinge.cemiuiler.module.securitycenter.SkipCountDownLimit;
import com.sevtinge.cemiuiler.module.securitycenter.UnlockSuperWirelessCharge;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDefaultSort;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDetails;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDisable;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppRestrict;
import com.sevtinge.cemiuiler.module.securitycenter.beauty.BeautyFace;
import com.sevtinge.cemiuiler.module.securitycenter.beauty.BeautyLight;
import com.sevtinge.cemiuiler.module.securitycenter.beauty.BeautyLightAuto;
import com.sevtinge.cemiuiler.module.securitycenter.beauty.BeautyPc;
import com.sevtinge.cemiuiler.module.securitycenter.beauty.BeautyPrivacy;
import com.sevtinge.cemiuiler.module.securitycenter.lab.AiClipboardEnable;
import com.sevtinge.cemiuiler.module.securitycenter.lab.BlurLocationEnable;
import com.sevtinge.cemiuiler.module.securitycenter.lab.GetNumberEnable;
import com.sevtinge.cemiuiler.module.securitycenter.sidebar.game.UnlockGunService;
import com.sevtinge.cemiuiler.module.securitycenter.sidebar.video.DisableRemoveScreenHoldOn;
import com.sevtinge.cemiuiler.module.securitycenter.sidebar.video.UnlockEnhanceContours;
import com.sevtinge.cemiuiler.module.securitycenter.sidebar.video.UnlockMemc;
import com.sevtinge.cemiuiler.module.securitycenter.sidebar.video.UnlockSuperResolution;

public class SecurityCenter extends BaseModule {

    @Override
    public void handleLoadPackage() {

        initHook(new SecurityCenterDexKit());

        // 应用管理
        initHook(new AppDefaultSort(), mPrefsMap.getStringAsInt("security_center_app_default_sort", 0) > 0);
        initHook(new AppRestrict(), mPrefsMap.getBoolean("security_center_app_restrict"));
        initHook(new AppDisable(), mPrefsMap.getBoolean("security_center_app_disable"));
        initHook(new AppDetails(), mPrefsMap.getBoolean("security_center_app_details"));
        initHook(new DisableReport(), mPrefsMap.getBoolean("security_center_disable_ban"));

        // 省电与电池
        // initHook(new ShowBatteryTemperature(), mPrefsMap.getBoolean("security_center_show_battery_temperature"));
        initHook(ShowBatteryTemperatureNew.INSTANCE, mPrefsMap.getBoolean("security_center_show_battery_temperature"));
        initHook(new UnlockSuperWirelessCharge(), mPrefsMap.getBoolean("security_center_super_wireless_charge"));
        initHook(ScreenUsedTime.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_screen_time"));

        // 隐私保护
        initHook(new AppLockPinScramble(), mPrefsMap.getBoolean("security_center_applock_pin_scramble"));
        initHook(new AiClipboardEnable(), mPrefsMap.getBoolean("security_center_ai_clipboard"));
        initHook(new BlurLocationEnable(), mPrefsMap.getBoolean("security_center_blur_location"));
        initHook(new GetNumberEnable(), mPrefsMap.getBoolean("security_center_get_number"));

        // 前置摄像助手
        initHook(BeautyLight.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_light"));
        initHook(new BeautyLightAuto(), mPrefsMap.getBoolean("security_center_beauty_light_auto"));
        initHook(new BeautyFace(), mPrefsMap.getBoolean("security_center_beauty_face"));
        initHook(BeautyPrivacy.INSTANCE, mPrefsMap.getBoolean("security_center_beauty_privacy"));
        initHook(new BeautyPc(), mPrefsMap.getBoolean("security_center_beauty_pc"));

        // 其他
        initHook(new LockOneHundredPoints(), mPrefsMap.getBoolean("security_center_score"));
        initHook(new SkipCountDownLimit(), mPrefsMap.getBoolean("security_center_skip_count_down_limit"));
        initHook(new DisableRootCheck(), mPrefsMap.getBoolean("security_center_disable_root_check"));
        initHook(FuckRiskPkg.INSTANCE, mPrefsMap.getBoolean("security_center_disable_send_malicious_app_notification"));

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
        initHook(new DisableRemoveScreenHoldOn(), mPrefsMap.getBoolean("security_center_disable_remove_screen_hold_on"));
        initHook(UnlockMemc.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_memc"));
        initHook(UnlockSuperResolution.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_s_resolution"));
        initHook(UnlockEnhanceContours.INSTANCE, mPrefsMap.getBoolean("security_center_unlock_enhance_contours"));
    }
}
