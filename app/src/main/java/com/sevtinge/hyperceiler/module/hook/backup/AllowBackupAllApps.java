package com.sevtinge.hyperceiler.module.hook.backup;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AllowBackupAllApps extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.backup.Customization", "isSkipDataApp", String.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
