package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.guardprovider.DisableUploadAppListNew;

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
