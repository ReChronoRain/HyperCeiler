package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import com.sevtinge.hyperceiler.common.log.XposedLog;

import io.github.libxposed.api.XposedModuleInterface;

public class DowngradeCheckPatch extends CorePatchHelper {
    private final String TAG = "DowngradeCheckPatch";

    public void init(XposedModuleInterface.SystemServerStartingParam lpparam) {
        // Android 13+
        // ee11a9c (Rename AndroidPackageApi to AndroidPackage)
        try {
            findAndHookMethod("com.android.server.pm.PackageManagerServiceUtils", lpparam.getClassLoader(),
                "checkDowngrade",
                "com.android.server.pm.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant("prefs_key_system_framework_core_patch_downgr", null));
        } catch (Throwable e) {
            XposedLog.e(TAG, "system", "Android 13+ hook failed, crash: " + e);
        }
    }
}
