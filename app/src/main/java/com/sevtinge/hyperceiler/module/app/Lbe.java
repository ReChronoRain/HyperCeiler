package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.lbe.DisableClipboardTip;

public class Lbe extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(DisableClipboardTip.INSTANCE, mPrefsMap.getBoolean("lbe_clipboard_tip_toast"));
    }
}

