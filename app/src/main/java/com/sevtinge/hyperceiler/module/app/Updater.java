package com.sevtinge.hyperceiler.module.app;

import android.text.TextUtils;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.updater.AndroidVersionCode;
import com.sevtinge.hyperceiler.module.hook.updater.DeviceModify;
import com.sevtinge.hyperceiler.module.hook.updater.VabUpdate;
import com.sevtinge.hyperceiler.module.hook.updater.VersionCodeModify;
import com.sevtinge.hyperceiler.module.hook.updater.VersionCodeNew;

public class Updater extends BaseModule {

    @Override
    public void handleLoadPackage() {
        if (mPrefsMap.getBoolean("updater_enable_miui_version")) {
            if (mPrefsMap.getStringAsInt("updater_version_mode", 1) != 1) {
                initHook(VersionCodeNew.INSTANCE);
            } else {
                initHook(new VersionCodeModify(), !TextUtils.isEmpty(mPrefsMap.getString("various_updater_miui_version", "")));
            }
            initHook(AndroidVersionCode.INSTANCE, !TextUtils.isEmpty(mPrefsMap.getString("various_updater_android_version", "")));
            initHook(DeviceModify.INSTANCE, !TextUtils.isEmpty(mPrefsMap.getString("updater_device", "")));
        }
        initHook(new VabUpdate(), mPrefsMap.getBoolean("updater_fuck_vab"));
    }
}
