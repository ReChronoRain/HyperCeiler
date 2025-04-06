package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.util.List;

public class AllowDisableProtectedPackage extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.server.pm.PackageManagerService", "setEnabledSettings", List.class, int.class, String.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod("com.android.server.pm.ProtectedPackages", "isPackageStateProtected", int.class, String.class, new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
            }
        });
    }
}
