package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.miinput.UnlockKnuckleFunction;

public class InputSettings extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(UnlockKnuckleFunction.INSTANCE, mPrefsMap.getBoolean("system_settings_knuckle_function"));
    }
}
