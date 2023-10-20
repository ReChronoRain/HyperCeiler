package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.base.CloseHostDir;
import com.sevtinge.cemiuiler.module.base.LoadHostDir;
import com.sevtinge.cemiuiler.module.hook.mediaeditor.FilterManagerAll;
import com.sevtinge.cemiuiler.module.hook.mediaeditor.UnlockMinimumCropLimitNew;

public class MediaEditor extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(UnlockMinimumCropLimitNew.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_minimum_crop_limit"));
        initHook(FilterManagerAll.INSTANCE, mPrefsMap.getBoolean("mediaeditor_filter_manager"));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }

}


