package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.browser.DebugMode;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

public class Browser extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(DebugMode.INSTANCE, mPrefsMap.getBoolean("browser_debug_mode"));
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
