package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.libxposed.api.XposedModuleInterface;

public class VerificationAgentPatch extends CorePatchHelper {

    private static final String TAG = "VerificationAgentPatch";

    public void init(XposedModuleInterface.SystemServerLoadedParam lpparam) {
        // Android 11+
        try {
            hookAllMethods(getIsVerificationEnabledClass(lpparam.getClassLoader()), "isVerificationEnabled", new ReturnConstant("prefs_key_system_framework_disable_verification_agent", false));
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ hook failed, crash: " + t);
        }
    }

    Class<?> getIsVerificationEnabledClass(ClassLoader classLoader) {
        if (isMoreAndroidVersion(34)) {
            return EzxHelpUtils.findClass("com.android.server.pm.VerifyingSession", classLoader);
        } else if (isAndroidVersion(33)) {
            return EzxHelpUtils.findClass("com.android.server.pm.VerificationParams", classLoader);
        }
        return EzxHelpUtils.findClass("com.android.server.pm.PackageManagerService", classLoader);
    }
}
