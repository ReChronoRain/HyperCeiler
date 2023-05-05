package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.lbe.DisableClipboardTip;

public class Lbe extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(DisableClipboardTip.INSTANCE, /*mPrefsMap.getBoolean("lbe_disable_clipboard_tip") ||*/
                mPrefsMap.getBoolean("lbe_clipboard_tip_toast"));
    }
}

