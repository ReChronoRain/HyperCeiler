package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.weather.SetDeviceLevel;

public class Weather extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new SetDeviceLevel(), mPrefsMap.getStringAsInt("weather_device_level", 0) != 3);
    }
}


