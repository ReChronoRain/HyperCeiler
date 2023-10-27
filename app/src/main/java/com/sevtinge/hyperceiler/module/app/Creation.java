package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.creation.UnlockCreation;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

public class Creation extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(UnlockCreation.INSTANCE, mPrefsMap.getBoolean("creation_unlock_enable"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
    }
}
