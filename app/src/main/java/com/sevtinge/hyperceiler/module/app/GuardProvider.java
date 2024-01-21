package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.guardprovider.DisableUploadAppListNew;

public class GuardProvider extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(DisableUploadAppListNew.INSTANCE, mPrefsMap.getBoolean("disable_upload_applist"));
    }
}
