package com.sevtinge.hyperceiler.module.hook.market;


import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;


public class NewIcon extends BaseHook {
    static Method isDesktopSupportOperationIcon;

    @Override
    public void init() {
       /* try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(MarketDexKit.mMarketResultMethodsMap.get("DesktopSupportOperationIcon"));
            for (DexMethodDescriptor descriptor : result) {
                isDesktopSupportOperationIcon = descriptor.getMethodInstance(lpparam.classLoader);
                logI("isDesktopSupportOperationIcon method is " + isDesktopSupportOperationIcon);
                if (isDesktopSupportOperationIcon.getReturnType() == boolean.class) {
                    XposedBridge.hookMethod(isDesktopSupportOperationIcon, XC_MethodReplacement.returnConstant(false));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        hookAllMethods("com.xiaomi.market.util.FileUtils", "ensureExternalIconDir", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(false);
            }
        });*/
    }
}
