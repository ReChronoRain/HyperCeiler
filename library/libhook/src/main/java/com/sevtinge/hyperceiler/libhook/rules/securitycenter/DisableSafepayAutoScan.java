package com.sevtinge.hyperceiler.libhook.rules.securitycenter;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class DisableSafepayAutoScan extends BaseHook {
    @Override
    public void init() {
        Method method = DexKit.findMember("GetKeySafepayAutoScanState", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("key_safepay_auto_scan_state")
                        .paramCount(0)
                    )).singleOrNull();
                methodData.toDexMethod().serialize();
                return methodData;
            }
        });
        hookMethod(method, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });
    }
}
