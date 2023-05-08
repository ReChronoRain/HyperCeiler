package com.sevtinge.cemiuiler.module;

import android.os.Build;
import android.util.Log;

import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForR;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForS;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForSv2;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForT;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap;
import static com.sevtinge.cemiuiler.utils.Helpers.log;

public class SystemFrameworkForCorepatch implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "Cemiuiler";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        log("handleLoadPackage loading.");
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android"))) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            log("Downgrade=" + mPrefsMap.getBoolean("system_framework_core_patch_downgr"));
            log("AuthCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_auth_creak"));
            log("DigestCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_digest_creak"));
            log("UsePreSig=" + mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature"));
            log("EnhancedMode=" + mPrefsMap.getBoolean("system_framework_core_patch_enhanced_mode"));
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().handleLoadPackage(lpparam);
                    log("CorePatchForT handleLoadPackage loaded.");
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                    new CorePatchForSv2().handleLoadPackage(lpparam);
                    log("CorePatchForSv2 handleLoadPackage loaded.");
                    break;
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().handleLoadPackage(lpparam);
                    log("CorePatchForS handleLoadPackage loaded.");
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().handleLoadPackage(lpparam);
                    log("CorePatchForR handleLoadPackage loaded.");
                    break;
                default:
                    log("Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        log("initZygote loading.");
        if (startupParam.startsSystemServer) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().initZygote(startupParam);
                    log("CorePatchForT initZygote loaded.");
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                    new CorePatchForSv2().initZygote(startupParam);
                    log("CorePatchForSv2 initZygote loaded.");
                    break;
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().initZygote(startupParam);
                    log("CorePatchForS initZygote loaded.");
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().initZygote(startupParam);
                    log("CorePatchForR initZygote loaded.");
                    break;
                default:
                    log("Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }
}
