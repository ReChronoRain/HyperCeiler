package com.sevtinge.cemiuiler.module.hook.browser;

import static com.sevtinge.cemiuiler.module.hook.browser.BrowserDexKit.mBrowserResultMethodsMap;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class DebugMode extends BaseHook {
    @Override
    public void init() {
        boolean found = false;
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(mBrowserResultMethodsMap.get("DebugMode"));
            for (DexMethodDescriptor descriptor : result) {
                Method DebugMode = descriptor.getMethodInstance(lpparam.classLoader);
                if (DebugMode.getReturnType() == boolean.class && String.valueOf(DebugMode).contains("getDebugMode")) {
                    logI("DebugMode method is " + DebugMode);
                    found = true;
                    XposedBridge.hookMethod(DebugMode, XC_MethodReplacement.returnConstant(true));
                }
            }
            if (!found) {
                List<DexMethodDescriptor> result1 = Objects.requireNonNull(mBrowserResultMethodsMap.get("DebugMode1"));
                for (DexMethodDescriptor descriptor1 : result1) {
                    Method DebugMode1 = descriptor1.getMethodInstance(lpparam.classLoader);
                    if (DebugMode1.getReturnType() == boolean.class && String.valueOf(DebugMode1).contains("getDebugMode")) {
                        logI("DebugMode1 method is " + DebugMode1);
                        found = true;
                        XposedBridge.hookMethod(DebugMode1, XC_MethodReplacement.returnConstant(true));
                    }
                }
            }
            if (!found) {
                List<DexMethodDescriptor> result2 = Objects.requireNonNull(mBrowserResultMethodsMap.get("DebugMode2"));
                for (DexMethodDescriptor descriptor2 : result2) {
                    Method DebugMode2 = descriptor2.getMethodInstance(lpparam.classLoader);
                    if (DebugMode2.getReturnType() == boolean.class && String.valueOf(DebugMode2).contains("getDebugMode")) {
                        logI("DebugMode2 method is " + DebugMode2);
                        XposedBridge.hookMethod(DebugMode2, XC_MethodReplacement.returnConstant(true));
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}


