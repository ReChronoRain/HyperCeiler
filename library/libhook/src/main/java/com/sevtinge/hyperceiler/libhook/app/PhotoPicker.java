package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.photopicker.DisableReroute;

@HookBase(targetPackage = "com.android.photopicker")
public class PhotoPicker extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        initHook(new DisableReroute(), PrefsBridge.getBoolean("photopicker_disable_reroute"));
    }
}
