package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

public class Notes extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getStringAsInt("various_super_clipboard_e", 0) != 0);
    }
}
