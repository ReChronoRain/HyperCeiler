package com.sevtinge.cemiuiler.module.packageinstaller;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DisableSafeModelTip extends BaseHook {
    @Override
    public void init() {

        findAndHookMethod("com.miui.packageInstaller.model.ApkInfo", "getSystemApp", XC_MethodReplacement.returnConstant(true));

        hookAllMethods("com.miui.packageInstaller.InstallProgressActivity", "g0", XC_MethodReplacement.returnConstant(false));

        XposedHelpers.findAndHookMethod("com.miui.packageInstaller.InstallProgressActivity", lpparam.classLoader, "Q1", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(new ArrayList<>());
            }
        });

        //returnIntConstant(findClassIfExists("p6.a"), "d");


    }


}
