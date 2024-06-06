package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.module.base.BaseTool;

public class DisablePersistent extends BaseTool {
    private boolean isInstall = false;

    @Override
    public void doHook() {
        hcHook.findClass("pl", "com.android.server.pm.parsing.pkg.PackageImpl")
                .getMethod("isPersistent")
                .hook(new IAction() {
                    @Override
                    public void after(ParamTool param) {
                        // String pkg = param.getField("packageName");
                        boolean isPersistent = param.getResult();
                        if (isPersistent) {
                            if (isInstall) {
                                param.setResult(false);
                            }
                        }
                        // logI(TAG, "pkg: " + pkg + " isPersistent: " + param.getResult());
                    }
                })
                .findClass("iph", "com.android.server.pm.InstallPackageHelper")
                .getMethod("preparePackageLI", "com.android.server.pm.InstallRequest")
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        isInstall = true;
                        // logI(TAG, "start install: " + param.first());
                    }

                    @Override
                    public void after(ParamTool param) {
                        isInstall = false;
                        // logI(TAG, "end install: " + param.first());
                    }
                })
        ;
    }
}
