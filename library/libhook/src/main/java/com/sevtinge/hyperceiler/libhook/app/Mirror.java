package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.mirror.UnlockSendAppM;

@HookBase(targetPackage = "com.xiaomi.mirror")
public class Mirror extends BaseLoad {

    public Mirror() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        initHook(UnlockSendAppM.INSTANCE, mPrefsMap.getBoolean("milink_unlock_send_app"));
    }
}
