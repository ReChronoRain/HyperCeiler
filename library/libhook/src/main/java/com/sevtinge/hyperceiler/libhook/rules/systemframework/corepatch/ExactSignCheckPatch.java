package com.sevtinge.hyperceiler.libhook.rules.systemframework.corepatch;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClassIfExists;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedModuleInterface;

public class ExactSignCheckPatch extends CorePatchHelper {

    private final String TAG = "ExactSignCheckPatch";

    public void init(XposedModuleInterface.SystemServerLoadedParam lpparam) {
        // Android 11+
        try {
            Class<?> signingDetails = getSigningDetails(lpparam.getClassLoader());
            // Allow apk splits with different signatures to be installed together
            hookAllMethods(signingDetails, "signaturesMatchExactly", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (prefs.getBoolean("prefs_key_system_framework_core_patch_exact_signature_check", false))
                        param.setResult(true);
                }
            });
        } catch (Throwable t) {
            XposedLog.e(TAG, "system", "Android 11+ hook failed, crash: " + t);
        }
    }

    Class<?> getSigningDetails(ClassLoader classLoader) {
        if (isMoreAndroidVersion(33)) {
            return findClassIfExists("android.content.pm.SigningDetails", classLoader);
        }
        return EzxHelpUtils.findClass("android.content.pm.PackageParser.SigningDetails", classLoader);
    }
}
