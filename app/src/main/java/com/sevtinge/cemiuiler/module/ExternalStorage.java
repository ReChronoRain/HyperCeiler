package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.externalstorage.DisableFolderCantUse;

public class ExternalStorage extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableFolderCantUse(), mPrefsMap.getBoolean("various_disable_folder_cantuse"));
    }
}