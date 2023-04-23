package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.securitycenter.*;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDefaultSort;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDetails;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDisable;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppRestrict;

import com.sevtinge.cemiuiler.module.securitycenter.beauty.*;
import com.sevtinge.cemiuiler.module.securitycenter.lab.AiClipboardEnable;
import com.sevtinge.cemiuiler.module.securitycenter.lab.BlurLocationEnable;
import com.sevtinge.cemiuiler.module.securitycenter.lab.GetNumberEnable;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SecurityCenter extends BaseModule {

    @Override
    public void handleLoadPackage() {

        initHook(new SecurityCenterDexKit());

        //应用管理
        initHook(new AppDefaultSort(), mPrefsMap.getStringAsInt("security_center_app_default_sort", 0) > 0);
        initHook(new AppRestrict(), mPrefsMap.getBoolean("security_center_app_restrict"));
        initHook(new AppDisable(), mPrefsMap.getBoolean("security_center_app_disable"));
        initHook(new AppDetails(), mPrefsMap.getBoolean("security_center_app_details"));

        //省电与电池
        //initHook(new ShowBatteryTemperature(), mPrefsMap.getBoolean("security_center_show_battery_temperature"));
        initHook(ShowBatteryTemperatureNew.INSTANCE, mPrefsMap.getBoolean("security_center_show_battery_temperature"));
        initHook(new UnlockSuperWirelessCharge(), mPrefsMap.getBoolean("security_center_super_wireless_charge"));

        //隐私保护
        initHook(new AppLockPinScramble(), mPrefsMap.getBoolean("security_center_applock_pin_scramble"));
        initHook(new AiClipboardEnable(), mPrefsMap.getBoolean("security_center_ai_clipboard"));
        initHook(new BlurLocationEnable(), mPrefsMap.getBoolean("security_center_blur_location"));
        initHook(new GetNumberEnable(), mPrefsMap.getBoolean("security_center_get_number"));

        //前置摄像助手
        initHook(new BeautyLight(), mPrefsMap.getBoolean("security_center_beauty_light"));
        initHook(new BeautyLightAuto(), mPrefsMap.getBoolean("security_center_beauty_light_auto"));
        initHook(new BeautyFace(), mPrefsMap.getBoolean("security_center_beauty_face"));
        initHook(new BeautyPrivacy(), mPrefsMap.getBoolean("security_center_beauty_privacy"));
        initHook(new BeautyPc(), mPrefsMap.getBoolean("security_center_beauty_pc"));

        //其他
        initHook(new LockOneHundredPoints());
        initHook(new SkipCountDownLimit(), mPrefsMap.getBoolean("security_center_skip_count_down_limit"));
        initHook(new DisableRootCheck(), mPrefsMap.getBoolean("security_center_disable_root_check"));
        initHook(new RemoveMacroBlackList(), mPrefsMap.getBoolean("security_center_remove_macro_black_list"));

        //小窗和气泡通知
        initHook(new RemoveConversationBubbleSettingsRestriction(), mPrefsMap.getBoolean("security_center_remove_conversation_bubble_settings_restriction"));
        initHook(IsSbnBelongToActiveBubbleApp.INSTANCE,mPrefsMap.getBoolean("security_center_unlock_side_hide_freeform"));
        initHook(GetBubbleAppString.INSTANCE,mPrefsMap.getBoolean("security_center_unlock_side_hide_freeform"));

        //移除打开应用弹窗
        initHook(new RemoveOpenAppConfirmationPopup(), mPrefsMap.getBoolean("security_center_remove_open_app_confirmation_popup"));

        //
        initHook(new NewBoxBlur(), mPrefsMap.getBoolean("security_center_newbox_custom_enable"));
        initHook(BlurSecurity.INSTANCE, mPrefsMap.getBoolean("se_enable"));

    }
}
