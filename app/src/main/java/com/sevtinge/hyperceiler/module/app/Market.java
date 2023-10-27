package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.market.DeviceModify;

public class Market extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DeviceModify(), mPrefsMap.getStringAsInt("market_device_modify_new", 0) != 0);;
    }
}
