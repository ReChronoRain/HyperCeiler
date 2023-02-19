package com.sevtinge.cemiuiler.module.joyose;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DisableCloudControl extends BaseHook {

    Class<?> mCloud;

    @Override
    public void init() {
        mCloud = findClassIfExists("com.xiaomi.joyose.cloud.g$a");

        findAndHookMethod(mCloud, "run", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                return;
            }
        });
    }
}
