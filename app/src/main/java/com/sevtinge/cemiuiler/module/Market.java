package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.market.DeviceModify;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Market extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DeviceModify(), mPrefsMap.getBoolean("market_device_modify"));
    }
}
