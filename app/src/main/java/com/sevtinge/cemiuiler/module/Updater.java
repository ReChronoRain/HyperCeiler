package com.sevtinge.cemiuiler.module;

import android.text.TextUtils;
import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.updater.*;

public class Updater extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new VersionCodeModify(), !TextUtils.isEmpty(mPrefsMap.getString("various_updater_miui_version","")));
        initHook(new VabUpdate(), mPrefsMap.getBoolean("updater_fuck_vab"));
        initHook(DeviceModify.INSTANCE, mPrefsMap.getBoolean("hidden_function") && !TextUtils.isEmpty(mPrefsMap.getString("updater_device","")));
    }
}
