package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.tsmclient.AutoNfc;

public class TsmClient extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(AutoNfc.INSTANCE, mPrefsMap.getBoolean("tsmclient_auto_nfc"));
    }
}
