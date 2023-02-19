package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.weather.SetDeviceLevel;

public class Weather extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new SetDeviceLevel(), mPrefsMap.getStringAsInt("weather_device_level", 0) != 3);
    }
}


