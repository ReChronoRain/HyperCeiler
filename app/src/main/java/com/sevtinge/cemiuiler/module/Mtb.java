package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.mtb.*;

public class Mtb extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(BypassAuthentication.INSTANCE, mPrefsMap.getBoolean("mtb_auth"));
        initHook(IsUserBuild.INSTANCE, mPrefsMap.getBoolean("mtb_auth"));
    }
}