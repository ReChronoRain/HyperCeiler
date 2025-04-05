package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisablePersistent extends BaseHook {
    private boolean isInstall = false;

    @Override
    public void init() {
        String packageName = isMoreAndroidVersion(35) ? "com.android.server.pm.PackageSetting"
            : "com.android.server.pm.parsing.pkg.PackageImpl";

        try {
            Class<?> mPackage = findClassIfExists(packageName);

            findAndHookMethod(mPackage, "isPersistent", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    boolean isPersistent = (boolean) param.getResult();
                    if (isPersistent && isInstall) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable t) {
            logE(TAG, lpparam.packageName, "Not found class: " + packageName);
        }

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
