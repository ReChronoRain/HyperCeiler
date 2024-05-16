package com.sevtinge.hyperceiler.module.hook.demo;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getPackageVersionCode;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitData;

import org.json.JSONException;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

public class DexKitTest extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        DexKitData.hookMethodWithDexKit("Key", lpparam,
                MethodMatcher.create()
                        .usingStrings("pref_key_debug_mode_" + getPackageVersionCode(lpparam))
                        .name("getDebugMode")
                        .returnType(boolean.class), new DexKitData.MethodHookWithDexKit() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });

    }
}
