package com.sevtinge.hyperceiler.module.hook.guardprovider;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;

public class DisableUploadAppListNew extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = (Method) DexKit.getDexKitBridge("AntiDefraudAppManager", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app")
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        hookMethod(method, new replaceHookedMethod() {
            @Override
            protected Object replace(MethodHookParam param) throws Throwable {
                return null;
            }
        });
    }
}
