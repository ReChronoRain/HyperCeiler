package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.creation.UnlockCreation;
import com.sevtinge.cemiuiler.module.hook.various.UnlockSuperClipboard;

public class Creation extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(UnlockCreation.INSTANCE, mPrefsMap.getBoolean("creation_unlock_enable"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
    }
}
