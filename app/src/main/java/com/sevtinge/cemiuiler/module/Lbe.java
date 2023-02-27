package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.lbe.DisableClipboardTip;

public class Lbe extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableClipboardTip(), mPrefsMap.getBoolean("lbe_disable_clipboard_tip"));
    }
}

