package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class AllowUntrustedTouch extends BaseHook {

    Class<?> mInputManager;

    @Override
    public void init() {
        mInputManager = findClassIfExists("android.hardware.input.InputManager");
        hookAllMethods(mInputManager, "getBlockUntrustedTouchesMode", XC_MethodReplacement.returnConstant(0));// error
    }
}
