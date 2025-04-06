package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin;

import static com.sevtinge.hyperceiler.hook.module.base.tool.HookTool.hookMethod;
import static com.sevtinge.hyperceiler.hook.utils.PropUtils.getProp;

import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;
import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

public class ShowDeviceName {

    static String deviceName = getProp("persist.sys.device_name");

    public static void initShowDeviceName(ClassLoader classLoader) {
        Method method = DexKit.findMember("OnCarrierTextChanged", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .name("onCarrierTextChanged")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method, new HookTool.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.args[0] = deviceName;
            }
        });
    }
}

