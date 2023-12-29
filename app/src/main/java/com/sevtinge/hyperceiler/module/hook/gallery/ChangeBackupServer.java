package com.sevtinge.hyperceiler.module.hook.gallery;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class ChangeBackupServer extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        boolean getBackupServer = mPrefsMap.getStringAsInt("gallery_backup_server", 0) == 2;
        findAndHookMethod("com.miui.gallery.transfer.GoogleSyncHelper", "isCloudServiceOffLine", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(getBackupServer);
            }
        });
    }
}
