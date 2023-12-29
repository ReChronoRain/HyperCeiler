package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class DisableLowApiCheckForU extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.server.pm.InstallPackageHelper", "preparePackageLI", "com.android.server.pm.InstallRequest", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object mInstallArgs = XposedHelpers.getObjectField(param.args[0], "mInstallArgs");
                if (mInstallArgs == null) return;
                XposedHelpers.setIntField(mInstallArgs, "mInstallFlags", XposedHelpers.getIntField(mInstallArgs, "mInstallFlags") | 0x01000000);
            }
        });
    }
}
