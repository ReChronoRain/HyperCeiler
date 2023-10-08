package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.base.CloseHostDir;
import com.sevtinge.cemiuiler.module.base.LoadHostDir;
import com.sevtinge.cemiuiler.module.hook.guardprovider.DisableUploadAppListNew;

public class GuardProvider extends BaseModule {
    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(DisableUploadAppListNew.INSTANCE, mPrefsMap.getBoolean("disable_upload_applist"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
