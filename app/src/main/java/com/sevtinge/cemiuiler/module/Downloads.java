package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.downloads.FuckXlDownload;

public class Downloads extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new FuckXlDownload(), mPrefsMap.getBoolean("various_fuck_xlDownload"));
    }
}