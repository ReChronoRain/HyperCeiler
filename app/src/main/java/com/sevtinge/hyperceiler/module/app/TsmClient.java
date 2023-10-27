package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.tsmclient.AutoNfc;

public class TsmClient extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(AutoNfc.INSTANCE, mPrefsMap.getBoolean("tsmclient_auto_nfc"));
    }
}
