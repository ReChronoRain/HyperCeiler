package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.market.NewIcon;
import com.sevtinge.cemiuiler.module.systemframework.AllowUninstall;
import com.sevtinge.cemiuiler.module.market.DeviceModify;

public class Market extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DeviceModify(), mPrefsMap.getBoolean("market_device_modify"));
        initHook(new NewIcon(), mPrefsMap.getBoolean("market_disable_new_icon"));
    }
}
