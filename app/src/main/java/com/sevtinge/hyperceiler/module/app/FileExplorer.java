package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.fileexplorer.SelectName;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

public class FileExplorer extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(SelectName.INSTANCE, mPrefsMap.getBoolean("file_explorer_can_selectable") || mPrefsMap.getBoolean("file_explorer_is_single_line"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
    }
}
