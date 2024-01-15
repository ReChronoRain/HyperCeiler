package com.sevtinge.hyperceiler.module.hook.browser;

import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionCode;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

public class EnableDebugEnvironment extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("pref_key_debug_mode_" + getPackageVersionCode(lpparam))
                .name("getDebugMode")
                .returnType(boolean.class)
            )
        ).singleOrThrow(() -> new IllegalStateException("EnableDebugEnvironment: Cannot found MethodData"));
        Method method = methodData.getMethodInstance(lpparam.classLoader);
        logD(TAG, lpparam.packageName, "getDebugMode() method is " + method);
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
