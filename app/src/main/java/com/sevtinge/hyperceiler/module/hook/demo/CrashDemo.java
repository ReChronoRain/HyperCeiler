package com.sevtinge.hyperceiler.module.hook.demo;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class CrashDemo extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        XposedHelpers.findAndHookMethod("com.hchen.demo.MainActivity", lpparam.classLoader,
            "crash", int.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    int o = (int) param.args[0];
                    param.args[0] = 0;
                    logE(TAG, "int: " + o);
                }
            }
        );
    }
}
