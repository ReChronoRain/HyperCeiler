package com.sevtinge.hyperceiler.libhook.rules.securitycenter;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DisableAutoTurnAccessibilityOff extends BaseHook {
    private Method mRemoveAccessibilityMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mRemoveAccessibilityMethod = requiredMember("RemoveAccessibility", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("try to remove: ")
                    )).singleOrNull();
                return methodData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        hookMethod(mRemoveAccessibilityMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(null);
            }
        });
    }
}
