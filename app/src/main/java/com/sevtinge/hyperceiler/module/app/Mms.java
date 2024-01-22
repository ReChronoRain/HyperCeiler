package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.mms.DisableAd;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

public class Mms extends BaseModule {
    @Override
    public void handleLoadPackage() {
        initHook(new DisableAd(), mPrefsMap.getBoolean("mms_disable_ad"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getStringAsInt("various_super_clipboard_e", 0) != 0);
    }
}
