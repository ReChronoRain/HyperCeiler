package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.browser.BrowserDexKit;
import com.sevtinge.cemiuiler.module.browser.DebugMode;
import com.sevtinge.cemiuiler.module.various.UnlockSuperClipboard;

public class Browser extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new BrowserDexKit());
        initHook(new DebugMode(), mPrefsMap.getBoolean("browser_debug_mode"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
    }
}
