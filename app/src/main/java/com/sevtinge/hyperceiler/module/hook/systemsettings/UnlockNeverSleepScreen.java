package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.content.Context;
import android.util.AttributeSet;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockNeverSleepScreen extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.android.settings.KeyguardTimeoutListPreference", Context.class, AttributeSet.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod("android.os.SystemProperties", "get", String.class, new MethodHook(){
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult("lcd");
                    }
                });
            }
        });
    }
}
