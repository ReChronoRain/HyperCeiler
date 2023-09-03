package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.fileexplorer.SelectName;
import com.sevtinge.cemiuiler.module.hook.various.UnlockSuperClipboard;

public class FileExplorer extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(SelectName.INSTANCE, mPrefsMap.getBoolean("file_explorer_can_selectable") || mPrefsMap.getBoolean("file_explorer_is_single_line"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getBoolean("various_super_clipboard_enable"));
    }
}
