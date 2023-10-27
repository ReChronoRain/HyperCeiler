package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.nfc.*;

public class Nfc extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableSound(), mPrefsMap.getBoolean("nfc_disable_sound"));
        initHook(new AllowInformationScreen(), mPrefsMap.getBoolean("nfc_allow_information_screen"));
    }
}
