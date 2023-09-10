package com.sevtinge.cemiuiler.module.hook.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.hook.securitycenter.SecurityCenterDexKit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class BeautyFace extends BaseHook {
    static Method beautyFace;

    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("BeautyFace"));
            for (DexMethodDescriptor descriptor : result) {
                beautyFace = descriptor.getMethodInstance(lpparam.classLoader);
                logI("beautyFace method is " + beautyFace);
                if (beautyFace.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(beautyFace, XC_MethodReplacement.returnConstant(true));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        /*
        int appVersionCode = getPackageVersionCode(lpparam);
        if (appVersionCode == 40000749) {
            findAndHookMethod("p5.f", "D", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else {
            findAndHookMethod("com.miui.gamebooster.beauty.i", "z", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
        */
    }
}
