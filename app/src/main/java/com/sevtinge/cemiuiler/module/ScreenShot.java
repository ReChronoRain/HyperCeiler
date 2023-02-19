package com.sevtinge.cemiuiler.module;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.screenshot.UnlockMinimumCropLimit;

public class ScreenShot extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockMinimumCropLimit(), mPrefsMap.getBoolean("screenshot_unlock_minimum_crop_limit"));
    }
}


