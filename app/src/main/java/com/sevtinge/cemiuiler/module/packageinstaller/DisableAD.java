package com.sevtinge.cemiuiler.module.packageinstaller;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sevtinge.cemiuiler.module.packageinstaller.PackageInstallerDexKit.mPackageInstallerResultMethodsMap;

public class DisableAD extends BaseHook {

    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mPackageInstallerResultMethodsMap.get("EnableAds"));
            for (DexMethodDescriptor descriptor : result) {
                Method enableAds = descriptor.getMethodInstance(lpparam.classLoader);
                log("enableAds method is "+ enableAds);
                if (enableAds.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(enableAds, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mPackageInstallerResultMethodsMap.get("AppStoreRecommend"));
            for (DexMethodDescriptor descriptor : result) {
                Method appStoreRecommend = descriptor.getMethodInstance(lpparam.classLoader);
                log("appStoreRecommend method is "+ appStoreRecommend);
                if (appStoreRecommend.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(appStoreRecommend, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mPackageInstallerResultMethodsMap.get("InstallerOpenSafetyModel"));
            for (DexMethodDescriptor descriptor : result) {
                Method installerOpenSafetyModel = descriptor.getMethodInstance(lpparam.classLoader);
                log("installerOpenSafetyModel method is "+ installerOpenSafetyModel);
                if (installerOpenSafetyModel.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(installerOpenSafetyModel, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
