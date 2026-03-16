package com.sevtinge.hyperceiler.libhook.app;


import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;

@HookBase(targetPackage = "com.xiaomi.subscreencenter")
public class SubScreenCenter extends BaseLoad {

    public SubScreenCenter() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
//        initHook(UnlockFoucsAuth.INSTANCE, PrefsBridge.getBoolean("subscreencenter_unlock_foucs_app_sign_white_list"));
    }
}
