package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.downloads.FuckXlDownload;

public class Downloads extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new FuckXlDownload(), mPrefsMap.getBoolean("various_fuck_xlDownload"));
    }
}
