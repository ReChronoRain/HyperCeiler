package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.FilterManagerAll;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockCustomPhotoFrames;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockDisney;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockLeicaFilter;
import com.sevtinge.hyperceiler.module.hook.mediaeditor.UnlockMinimumCropLimitNew;

public class MediaEditor extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);

        // 基础
        initHook(UnlockMinimumCropLimitNew.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_minimum_crop_limit"));
        initHook(FilterManagerAll.INSTANCE, mPrefsMap.getBoolean("mediaeditor_filter_manager"));
        initHook(UnlockLeicaFilter.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_leica_filter"));
        // AI 创作
        initHook(UnlockCustomPhotoFrames.INSTANCE, mPrefsMap.getStringAsInt("mediaeditor_unlock_custom_photo_frames", 0) != 0);
        initHook(UnlockDisney.INSTANCE, mPrefsMap.getBoolean("mediaeditor_unlock_disney"));

        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }

}


