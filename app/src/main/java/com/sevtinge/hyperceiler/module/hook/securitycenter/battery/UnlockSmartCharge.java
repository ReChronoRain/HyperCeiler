package com.sevtinge.hyperceiler.module.hook.securitycenter.battery;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

public class UnlockSmartCharge extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = (Method) DexKit.getDexKitBridge("SmartCharge", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("persist.vendor.smartchg")
                        )).firstOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
