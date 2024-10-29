package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.callback.IAction;

public class DisablePersistent extends BaseHC {
    private boolean isInstall = false;

    @Override
    public void init() {
        hook("com.android.server.pm.parsing.pkg.PackageImpl", "isPersistent",
                new IAction() {
                    @Override
                    public void after() throws Throwable {
                        boolean isPersistent = getResult();
                        if (isPersistent) {
                            if (isInstall) {
                                returnFalse();
                            }
                        }
                    }
                });
        
        hook("com.android.server.pm.InstallPackageHelper",
                "preparePackageLI", "com.android.server.pm.InstallRequest",
                new IAction() {
                    @Override
                    public void before() throws Throwable {
                        isInstall = true;
                    }

                    @Override
                    public void after() throws Throwable {
                        isInstall = false;
                    }
                });
    }
}
