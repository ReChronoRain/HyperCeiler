package com.sevtinge.hyperceiler.module.app;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.HookExpand;
import com.sevtinge.hyperceiler.module.hook.demo.ColorTest;
import com.sevtinge.hyperceiler.module.hook.demo.CrashDemo;
import com.sevtinge.hyperceiler.module.hook.demo.ToastTest;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

@HookExpand(pkg = "com.hchen.demo", isPad = false, tarAndroid = 34)
public class Demo extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new ToastTest(), true);
        initHook(new CrashDemo(), true);
        initHook(new ColorTest(), true);
    }
}
