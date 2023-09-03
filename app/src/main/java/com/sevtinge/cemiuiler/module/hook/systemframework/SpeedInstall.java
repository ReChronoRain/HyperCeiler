package com.sevtinge.cemiuiler.module.hook.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class SpeedInstall extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("android.content.pm.PackageInstaller", "isSupportedSpeedInstallV2", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
