package com.sevtinge.cemiuiler.module.packageinstaller;

import static com.sevtinge.cemiuiler.module.packageinstaller.PackageInstallerDexKit.mPackageInstallerResultMethodsMap;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class InstallRiskDisable extends BaseHook {


    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mPackageInstallerResultMethodsMap.get("SecureVerifyEnable"));
            for (DexMethodDescriptor descriptor : result) {
                Method secureVerifyEnable = descriptor.getMethodInstance(lpparam.classLoader);
                log("secureVerifyEnable method is " + secureVerifyEnable);
                if (secureVerifyEnable.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(secureVerifyEnable, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        findAndHookMethod("com.android.packageinstaller.compat.MiuiSettingsCompat",
            "isInstallRiskEnabled",
            Context.class,
            XC_MethodReplacement.returnConstant(false));

        findAndHookMethod("com.android.packageinstaller.compat.MiuiSettingsCompat",
            "isPersonalizedAdEnabled",
            XC_MethodReplacement.returnConstant(false));
    }
}
