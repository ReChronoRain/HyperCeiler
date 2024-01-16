package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.demo.CrashDemo;

public class Demo extends BaseModule {
    @Override
    public void handleLoadPackage() {

        initHook(new CrashDemo(), true);

        // AndroidLogUtils.LogI(ITag.TAG(), "im runn");
    }
}
