package com.sevtinge.hyperceiler.module.hook.powerkeeper;

import androidx.annotation.NonNull;

import com.github.kyuubiran.ezxhelper.HookFactory;
import com.github.kyuubiran.ezxhelper.interfaces.IMethodHookCallback;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.DexKit;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;

public class PreventBatteryWitelist extends BaseHook {
    @Override
    public void init() {
        // hookAllMethods("com.miui.powerkeeper.utils.CommonAdapter", lpparam.classLoader, "addPowerSaveWhitelistApps", new MethodHook(20000) {
        //     @Override
        //     protected void before(MethodHookParam param) throws Throwable {
        //         param.setResult(null);
        //     }
        // });

        MethodData methodData = DexKit.INSTANCE.getDexKitBridge().findMethod(
            FindMethod.create()
                .matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create()
                        .usingStrings("addPowerSaveWhitelistApps: "))
                    .usingStrings("addPowerSaveWhitelistApps: ")
                )
        ).firstOrThrow(() -> new IllegalStateException("PreventBatteryWitelist: No class found MethodData"));
        try {
            HookFactory.createMethodHook(methodData.getMethodInstance(lpparam.classLoader), new Consumer<HookFactory>() {
                @Override
                public void accept(HookFactory hookFactory) {
                    hookFactory.before(new IMethodHookCallback() {
                        @Override
                        public void onMethodHooked(@NonNull XC_MethodHook.MethodHookParam methodHookParam) {
                            String[] strArr = (String[]) methodHookParam.args[0];
                            if (strArr.length > 1) {
                                methodHookParam.setResult(null);
                            }
                        }
                    });
                }
            });
        } catch (NoSuchMethodException e) {
            logE(TAG, this.lpparam.packageName, "NoSuchMethodException: " + e);
        }
    }
}
