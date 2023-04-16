package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.market.NewIcon;
import com.sevtinge.cemiuiler.module.systemframework.AllowUninstall;
import com.sevtinge.cemiuiler.module.market.DeviceModify;

public class Market extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DeviceModify(), mPrefsMap.getStringAsInt("market_device_modify_new", 0) != 0);
        initHook(new NewIcon(), mPrefsMap.getBoolean("market_disable_new_icon"));
    }
}
