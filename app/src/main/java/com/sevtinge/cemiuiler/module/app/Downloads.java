package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.downloads.FuckXlDownload;

public class Downloads extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new FuckXlDownload(), mPrefsMap.getBoolean("various_fuck_xlDownload"));
    }
}
