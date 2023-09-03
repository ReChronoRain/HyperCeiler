package com.sevtinge.cemiuiler.module.app;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.mediaeditor.FilterManagerAll;
import com.sevtinge.cemiuiler.module.hook.mediaeditor.MediaEditorDexKit;
import com.sevtinge.cemiuiler.module.hook.mediaeditor.UnlockMinimumCropLimit;

public class MediaEditor extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // XposedBridge.log("Cemiuiler: debug.");
        initHook(new MediaEditorDexKit());
        initHook(new UnlockMinimumCropLimit(), mPrefsMap.getBoolean("mediaeditor_unlock_minimum_crop_limit"));
        initHook(FilterManagerAll.INSTANCE, mPrefsMap.getBoolean("mediaeditor_filter_manager"));
    }


}


