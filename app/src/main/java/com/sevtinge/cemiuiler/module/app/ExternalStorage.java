package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.externalstorage.DisableFolderCantUse;

public class ExternalStorage extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableFolderCantUse(), mPrefsMap.getBoolean("various_disable_folder_cantuse"));
    }
}
