package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.fileexplorer.SelectName;

public class FileExplorer extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(SelectName.INSTANCE, mPrefsMap.getBoolean("file_explorer_can_selectable") || mPrefsMap.getBoolean("file_explorer_is_single_line"));
    }
}
