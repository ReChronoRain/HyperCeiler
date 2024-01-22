package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.milink.AllowCameraDevices;
import com.sevtinge.hyperceiler.module.hook.milink.FuckHpplay;
import com.sevtinge.hyperceiler.module.hook.milink.UnlockHMind;
import com.sevtinge.hyperceiler.module.hook.milink.UnlockMiShare;

public class MiLink extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockMiShare(), mPrefsMap.getBoolean("milink_unlock_mishare"));
        initHook(new UnlockHMind(), mPrefsMap.getBoolean("milink_unlock_hmind"));
        initHook(new AllowCameraDevices(), mPrefsMap.getBoolean("milink_allow_camera_devices"));
        initHook(new FuckHpplay(), mPrefsMap.getBoolean("milink_fuck_hpplay"));
    }
}

