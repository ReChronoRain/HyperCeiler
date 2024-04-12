package com.sevtinge.hyperceiler.module.hook.aod;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class UnlockAlwaysOnDisplay implements IXposedHookZygoteInit {
    private static final String TAG = "UnlockAlwaysOnDisplay";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        try {
            ClassLoader classLoader = startupParam.getClass().getClassLoader();
            XposedHelpers.findAndHookMethod("miui.util.FeatureParser", classLoader, "getBoolean",
                    String.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log(TAG + " " + " key: " + param.args[0] + " def: " + param.args[1]);
                            String key = (String) param.args[0];
                            if ("is_only_support_keycode_goto".equals(key)) {
                                param.setResult(false);
                            }
                        }
                    }
            );
            XposedBridge.log(TAG + " " + "Hook Done");
        } catch (Throwable e) {
            XposedBridge.log(TAG + " " + "Hook Error: " + e);
        }
    }
}
