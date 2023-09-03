package com.sevtinge.cemiuiler.module.app;

import android.text.TextUtils;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.updater.DeviceModify;
import com.sevtinge.cemiuiler.module.hook.updater.UpdaterDexKit;
import com.sevtinge.cemiuiler.module.hook.updater.VabUpdate;
import com.sevtinge.cemiuiler.module.hook.updater.VersionCodeModify;

public class Updater extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UpdaterDexKit());
        initHook(new VersionCodeModify(), !TextUtils.isEmpty(mPrefsMap.getString("various_updater_miui_version", "")));
        initHook(new VabUpdate(), mPrefsMap.getBoolean("updater_fuck_vab"));
        initHook(DeviceModify.INSTANCE, !TextUtils.isEmpty(mPrefsMap.getString("updater_device", "")));
    }
}
