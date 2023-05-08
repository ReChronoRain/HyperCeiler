package com.sevtinge.cemiuiler.module.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BeautyLightAuto extends BaseHook {
    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("BeautyLightAuto"));
            for (DexMethodDescriptor descriptor : result) {
                if (!String.valueOf(descriptor).contains("<clinit>")) {
                    Method beautyLightAuto = descriptor.getMethodInstance(lpparam.classLoader);
                    if (beautyLightAuto.getReturnType() == boolean.class && !String.valueOf(descriptor).contains(String.valueOf(BeautyFace.beautyFace))) {
                        log("beautyLightAuto method is " + beautyLightAuto);
                        XposedBridge.hookMethod(beautyLightAuto, XC_MethodReplacement.returnConstant(true));
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        /*
        int appVersionCode = getPackageVersionCode(lpparam);
        if (appVersionCode == 40000749) {
            findAndHookMethod("q7.m", "c", XC_MethodReplacement.returnConstant(true));
        } else {
            findAndHookMethod("com.miui.gamebooster.beauty.i", "i", XC_MethodReplacement.returnConstant(true));
        }
            /*findAndHookMethod("com.miui.gamebooster.utils.i", "i", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
       */

    }
}



