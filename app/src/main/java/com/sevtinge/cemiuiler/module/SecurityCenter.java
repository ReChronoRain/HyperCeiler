package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.securitycenter.*;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDefaultSort;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDetails;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppDisable;
import com.sevtinge.cemiuiler.module.securitycenter.app.AppRestrict;

import com.sevtinge.cemiuiler.module.securitycenter.beauty.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SecurityCenter extends BaseModule {

    @Override
    public void handleLoadPackage() {

        //应用管理
        initHook(new AppDefaultSort(), mPrefsMap.getStringAsInt("security_center_app_default_sort", 0) > 0);
        initHook(new AppRestrict(), mPrefsMap.getBoolean("security_center_app_restrict"));
        initHook(new AppDisable(), mPrefsMap.getBoolean("security_center_app_disable"));
        initHook(new AppDetails(), mPrefsMap.getBoolean("security_center_app_details"));

        //省电与电池
        initHook(new ShowBatteryTemperature(), mPrefsMap.getBoolean("security_center_show_battery_temperature"));

        //隐私保护
        initHook(new AppLockPinScramble(), mPrefsMap.getBoolean("security_center_applock_pin_scramble"));

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

        //
        initHook(new NewBoxBlur(), mPrefsMap.getBoolean("security_center_newbox_custom_enable"));

    }
}
