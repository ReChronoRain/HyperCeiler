package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.backup.AllowBackupAllApps;

public class Backup extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new AllowBackupAllApps(), mPrefsMap.getBoolean("backup_allow_all_apps"));
    }
}
