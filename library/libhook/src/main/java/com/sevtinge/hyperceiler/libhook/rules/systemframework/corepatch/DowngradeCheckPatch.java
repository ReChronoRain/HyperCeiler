package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClassIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findMethodExactIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.hookMethod;

import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.libxposed.api.XposedModuleInterface;

public class DowngradeCheckPatch extends CorePatchHelper {
    private final String TAG = "DowngradeCheckPatch";

    public void init(XposedModuleInterface.SystemServerLoadedParam lpparam) {
        // Android 15+
        try {
            Class<?> PackageManagerServiceUtils =
                findClassIfExists("com.android.server.pm.PackageManagerServiceUtils",
                    lpparam.getClassLoader());

            var checkDowngradeAlt = findMethodExactIfExists(PackageManagerServiceUtils,
                "checkDowngrade",
                "com.android.server.pm.PackageSetting",
                "android.content.pm.PackageInfoLite");
            if (checkDowngradeAlt != null) {
                hookMethod(checkDowngradeAlt, new ReturnConstant("prefs_key_system_framework_core_patch_downgr", null));
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 15+ hook failed, crash: " + t);
        }

        // Android 13+
        // ee11a9c (Rename AndroidPackageApi to AndroidPackage)
        try {
            findAndHookMethod("com.android.server.pm.PackageManagerServiceUtils", lpparam.getClassLoader(),
                "checkDowngrade",
                "com.android.server.pm.pkg.AndroidPackage",
                "android.content.pm.PackageInfoLite",
                new ReturnConstant("prefs_key_system_framework_core_patch_downgr", null));
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 13+ hook failed, crash: " + t);
        }

        // Android 11+
        try {
            if (isAndroidVersion(30)) {
                var pmService =
                    findClassIfExists("com.android.server.pm.PackageManagerService", lpparam.getClassLoader());
                if (pmService != null) {
                    var checkDowngrade = findMethodExactIfExists(pmService, "checkDowngrade",
                        "com.android.server.pm.parsing.pkg.AndroidPackage",
                        "android.content.pm.PackageInfoLite");
                    if (checkDowngrade != null) {
                        // 允许降级
                        hookMethod(checkDowngrade, new ReturnConstant("prefs_key_system_framework_core_patch_downgr", null));
                    }
                }
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ hook failed, crash: " + t);
        }
    }
}
