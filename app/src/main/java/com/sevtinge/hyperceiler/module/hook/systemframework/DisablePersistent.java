package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisablePersistent extends BaseHook {
    private boolean isInstall = false;

    @Override
    public void init() {
        findAndHookMethod("com.android.server.pm.parsing.pkg.PackageImpl", "isPersistent", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        boolean isPersistent = (boolean) param.getResult();
                        if (isPersistent) {
                            if (isInstall) {
                                param.setResult(false);
                            }
                        }
                    }
                });

        findAndHookMethod("com.android.server.pm.InstallPackageHelper", "preparePackageLI", "com.android.server.pm.InstallRequest", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                isInstall = true;
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                isInstall = false;
            }
        });
    }
}
