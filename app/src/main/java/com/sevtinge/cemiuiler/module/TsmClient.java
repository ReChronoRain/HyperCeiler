package com.sevtinge.cemiuiler.module;

import android.text.TextUtils;
import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.tsmclient.AutoNfc;

public class TsmClient extends BaseModule {

    @Override
    public void handleLoadPackage() {
        //initHook(new AutoNfc(), mPrefsMap.getBoolean("tsmclient_auto_nfc"));
    }
}
