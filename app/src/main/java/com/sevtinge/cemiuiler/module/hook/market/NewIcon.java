package com.sevtinge.cemiuiler.module.hook.market;

import static com.sevtinge.cemiuiler.module.hook.market.MarketDexKit.mMarketResultMethodsMap;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class NewIcon extends BaseHook {
    static Method isDesktopSupportOperationIcon;

    @Override
    public void init() {
        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(MarketDexKit.mMarketResultMethodsMap.get("DesktopSupportOperationIcon"));
            for (DexMethodDescriptor descriptor : result) {
                isDesktopSupportOperationIcon = descriptor.getMethodInstance(lpparam.classLoader);
                log("isDesktopSupportOperationIcon method is " + isDesktopSupportOperationIcon);
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
        });
    }
}
