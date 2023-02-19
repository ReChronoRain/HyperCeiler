package com.sevtinge.cemiuiler.module.systemframework.corepatch;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.InvocationTargetException;

public class CorePatchForS extends CorePatchForR {

    @Override
    public void init() {
        super.init();

        if (mPrefsMap.getBoolean("system_framework_core_patch_digest_creak") && mPrefsMap.getBoolean("system_framework_core_patch_use_pre_signature")) {
            findAndHookMethod("com.android.server.pm.PackageManagerService", "doesSignatureMatchForPermissions", String.class, "com.android.server.pm.parsing.pkg.ParsedPackage", int.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    //If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                    if (param.getResult().equals(false)) {
                        String pPname = (String) XposedHelpers.callMethod(param.args[1], "getPackageName");
                        if (pPname.contentEquals((String) param.args[0])) {
                            param.setResult(true);
                        }
                    }
                }
            });
        }
    }

    }
