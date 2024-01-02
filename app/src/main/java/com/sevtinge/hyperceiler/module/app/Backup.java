package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.backup.AllowBackupAllApps;
import com.sevtinge.hyperceiler.module.hook.backup.UnlockBrokenScreenBackup;

public class Backup extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UnlockBrokenScreenBackup(), mPrefsMap.getBoolean("backup_unlock_broken_screen_backup"));
        initHook(new AllowBackupAllApps(), mPrefsMap.getBoolean("backup_allow_all_apps"));
    }
}
