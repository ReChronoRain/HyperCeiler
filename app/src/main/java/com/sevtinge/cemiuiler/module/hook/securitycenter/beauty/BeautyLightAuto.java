package com.sevtinge.cemiuiler.module.hook.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.hook.securitycenter.SecurityCenterDexKit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

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



