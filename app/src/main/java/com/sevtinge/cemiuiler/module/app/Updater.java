package com.sevtinge.cemiuiler.module.app;

import android.text.TextUtils;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.base.CloseHostDir;
import com.sevtinge.cemiuiler.module.base.LoadHostDir;
import com.sevtinge.cemiuiler.module.hook.updater.DeviceModify;
import com.sevtinge.cemiuiler.module.hook.updater.VabUpdate;
import com.sevtinge.cemiuiler.module.hook.updater.VersionCodeModify;
import com.sevtinge.cemiuiler.module.hook.updater.VersionCodeNew;

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
