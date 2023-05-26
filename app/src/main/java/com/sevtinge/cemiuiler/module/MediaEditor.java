package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.mediaeditor.FilterManagerAll;
import com.sevtinge.cemiuiler.module.mediaeditor.MediaEditorDexKit;
import com.sevtinge.cemiuiler.module.mediaeditor.UnlockMinimumCropLimit;

public class MediaEditor extends BaseModule {

        @Override
        public void handleLoadPackage() {
            //XposedBridge.log("Cemiuiler: debug.");
            initHook(new MediaEditorDexKit());
            initHook(new UnlockMinimumCropLimit(), mPrefsMap.getBoolean("mediaeditor_unlock_minimum_crop_limit"));
            initHook(FilterManagerAll.INSTANCE, mPrefsMap.getBoolean("mediaeditor_filter_manager"));
        }


}


