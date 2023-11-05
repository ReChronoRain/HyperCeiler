package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AllowUntrustedTouch extends BaseHook {

    Class<?> mInputManager;

    @Override
    public void init() {
        mInputManager = findClassIfExists("android.hardware.input.InputManager");
        hookAllMethods(mInputManager, "getBlockUntrustedTouchesMode", MethodHook.returnConstant(0));// error
    }
}
