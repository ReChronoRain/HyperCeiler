package com.sevtinge.cemiuiler.module;

import android.os.Build;
import android.util.Log;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForR;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForS;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForSv2;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForT;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.sevtinge.cemiuiler.module.base.BaseHook.mPrefsMap;

public class SystemFrameworkForCorepatch implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static final String TAG = "Cemiuiler";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("Cemiuiler: CorePatch handleLoadPackage loading.");
        if (("android".equals(lpparam.packageName)) && (lpparam.processName.equals("android"))) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            XposedBridge.log("Cemiuiler: CorePatch Downgrade=" + mPrefsMap.getBoolean("system_framework_core_patch_downgr"));
            XposedBridge.log("Cemiuiler: CorePatch AuthCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_auth_creak"));
            XposedBridge.log("Cemiuiler: CorePatch DigestCreak=" + mPrefsMap.getBoolean("system_framework_core_patch_digest_creak"));
            XposedBridge.log("Cemiuiler: CorePatch UsePreSig=" + mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature"));
            XposedBridge.log("Cemiuiler: CorePatch EnhancedMode=" + mPrefsMap.getBoolean("system_framework_core_patch_enhanced_mode"));
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().handleLoadPackage(lpparam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForT handleLoadPackage loaded.");
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                    new CorePatchForSv2().handleLoadPackage(lpparam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForSv2 handleLoadPackage loaded.");
                    break;
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().handleLoadPackage(lpparam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForS handleLoadPackage loaded.");
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().handleLoadPackage(lpparam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForR handleLoadPackage loaded.");
                    break;
                default:
                    XposedBridge.log("Cemiuiler: CorePatch Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log("Cemiuiler: CorePatch initZygote loading.");
        if (startupParam.startsSystemServer) {
            Log.d(TAG, "Current sdk version " + Build.VERSION.SDK_INT);
            switch (Build.VERSION.SDK_INT) {
                case Build.VERSION_CODES.TIRAMISU: // 33
                    new CorePatchForT().initZygote(startupParam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForT initZygote loaded.");
                    break;
                case Build.VERSION_CODES.S_V2: // 32
                    new CorePatchForSv2().initZygote(startupParam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForSv2 initZygote loaded.");
                    break;
                case Build.VERSION_CODES.S: // 31
                    new CorePatchForS().initZygote(startupParam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForS initZygote loaded.");
                    break;
                case Build.VERSION_CODES.R: // 30
                    new CorePatchForR().initZygote(startupParam);
                    XposedBridge.log("Cemiuiler: CorePatch CorePatchForR initZygote loaded.");
                    break;
                default:
                    XposedBridge.log("Cemiuiler: CorePatch Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                    break;
            }
        }
    }
}
