package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.lbe.DisableClipboardTip;

public class Lbe extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(DisableClipboardTip.INSTANCE, mPrefsMap.getBoolean("lbe_clipboard_tip_toast"));
    }
}

