package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.mms.DisableAd;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

public class Mms extends BaseModule {
    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(DisableAd.INSTANCE, mPrefsMap.getBoolean("mms_disable_ad"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
