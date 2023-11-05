package com.sevtinge.hyperceiler.module.app;

import android.text.TextUtils;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.screenshot.DeviceShellCustomize;
import com.sevtinge.hyperceiler.module.hook.screenshot.SaveToPictures;
import com.sevtinge.hyperceiler.module.hook.screenshot.UnlockMinimumCropLimit;
import com.sevtinge.hyperceiler.module.hook.screenshot.UnlockPrivacyMarking;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

public class ScreenShot extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        initHook(UnlockMinimumCropLimit.INSTANCE, mPrefsMap.getBoolean("screenshot_unlock_minimum_crop_limit"));
        initHook(SaveToPictures.INSTANCE, mPrefsMap.getBoolean("screenshot_save_to_pictures"));
        initHook(DeviceShellCustomize.INSTANCE, !TextUtils.isEmpty(mPrefsMap.getString("screenshot_device_customize", "")));
        initHook(UnlockPrivacyMarking.INSTANCE, mPrefsMap.getBoolean("screenshot_unlock_privacy_marking"));
        // 超级剪切板
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getStringAsInt("various_super_clipboard_e", 0) != 0);
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}


