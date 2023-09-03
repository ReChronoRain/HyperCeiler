package com.sevtinge.cemiuiler.module.hook.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.hook.securitycenter.SecurityCenterDexKit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class BeautyPc extends BaseHook {
    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("BeautyPc"));
            for (DexMethodDescriptor descriptor : result) {
                Method beautyPc = descriptor.getMethodInstance(lpparam.classLoader);
                log("beautyPc method is " + beautyPc);
                if (beautyPc.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(beautyPc, XC_MethodReplacement.returnConstant(true));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        /*
        int appVersionCode = getPackageVersionCode(lpparam);
        if (appVersionCode == 40000749) {
            findAndHookMethod("p5.f", "V", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else {
            findAndHookMethod("com.miui.gamebooster.beauty.i", "L", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
        */
    }
}



