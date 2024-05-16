package com.sevtinge.hyperceiler.module.hook.browser;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getPackageVersionCode;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitData;

import org.luckypray.dexkit.query.matchers.MethodMatcher;

public class DebugMode extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        DexKitData.hookMethodWithDexKit("EnvironmentFlag", lpparam,
                MethodMatcher.create()
                        .usingStrings("environment_flag")
                        .returnType(String.class),
                new DexKitData.MethodHookWithDexKit() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(1);
                    }
                });
        DexKitData.hookMethodWithDexKit("DebugMode0", lpparam,
                MethodMatcher.create()
                        .usingStrings("pref_key_debug_mode_new")
                        .returnType(boolean.class),
                new DexKitData.MethodHookWithDexKit() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
        DexKitData.hookMethodWithDexKit("DebugMode1", lpparam,
                MethodMatcher.create()
                        .usingStrings("pref_key_debug_mode")
                        .returnType(boolean.class),
                new DexKitData.MethodHookWithDexKit() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
        DexKitData.hookMethodWithDexKit("Key", lpparam,
                MethodMatcher.create()
                        .usingStrings("pref_key_debug_mode_" + getPackageVersionCode(lpparam))
                        .returnType(boolean.class), new DexKitData.MethodHookWithDexKit() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
    }
}
