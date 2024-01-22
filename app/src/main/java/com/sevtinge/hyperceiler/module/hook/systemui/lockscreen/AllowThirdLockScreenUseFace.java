package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AllowThirdLockScreenUseFace extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.keyguard.KeyguardUpdateMonitor", "isUnlockWithFacePossible", int.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("miui.stub.MiuiStub$3", "isUnlockWithFingerprintPossible", int.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
