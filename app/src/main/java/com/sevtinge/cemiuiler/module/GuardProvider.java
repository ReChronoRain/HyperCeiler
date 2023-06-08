package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.guardprovider.DisableUploadAppListNew;
import com.sevtinge.cemiuiler.module.guardprovider.GuardProviderDexKit;

public class GuardProvider extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new GuardProviderDexKit());
        // initHook(new DisableUploadAppList(), mPrefsMap.getBoolean("disable_upload_applist"));
        initHook(new DisableUploadAppListNew(), mPrefsMap.getBoolean("disable_upload_applist"));
    }
}
