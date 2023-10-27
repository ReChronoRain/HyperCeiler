package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.externalstorage.DisableFolderCantUse;

public class ExternalStorage extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableFolderCantUse(), mPrefsMap.getBoolean("various_disable_folder_cantuse"));
    }
}
