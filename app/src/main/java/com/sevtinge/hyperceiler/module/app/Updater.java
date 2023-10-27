package com.sevtinge.hyperceiler.module.app;

import android.text.TextUtils;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.CloseHostDir;
import com.sevtinge.hyperceiler.module.base.LoadHostDir;
import com.sevtinge.hyperceiler.module.hook.updater.DeviceModify;
import com.sevtinge.hyperceiler.module.hook.updater.VabUpdate;
import com.sevtinge.hyperceiler.module.hook.updater.VersionCodeModify;
import com.sevtinge.hyperceiler.module.hook.updater.VersionCodeNew;

public class Updater extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // dexKit load
        initHook(LoadHostDir.INSTANCE);
        if (mPrefsMap.getStringAsInt("updater_version_mode", 1) != 1) {
            initHook(VersionCodeNew.INSTANCE);
        } else {
            initHook(new VersionCodeModify(), !TextUtils.isEmpty(mPrefsMap.getString("various_updater_miui_version", "")));
        }
        initHook(new VabUpdate(), mPrefsMap.getBoolean("updater_fuck_vab"));
        initHook(DeviceModify.INSTANCE, !TextUtils.isEmpty(mPrefsMap.getString("updater_device", "")));
        // dexKit finish
        initHook(CloseHostDir.INSTANCE);
    }
}
