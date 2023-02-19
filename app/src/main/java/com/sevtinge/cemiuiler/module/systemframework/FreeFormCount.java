package com.sevtinge.cemiuiler.module.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class FreeFormCount extends BaseHook {

    Class<?> mMiuiFreeFormActivityStack;
    Class<?> mMiuiFreeFormStackDisplayStrategy;
    Class<?> mMiuiFreeFormManagerService;

    @Override
    public void init() {

        mMiuiFreeFormActivityStack = findClassIfExists("com.android.server.wm.MiuiFreeFormActivityStack");
        mMiuiFreeFormStackDisplayStrategy = findClassIfExists("com.android.server.wm.MiuiFreeFormStackDisplayStrategy");
        mMiuiFreeFormManagerService = findClassIfExists("com.android.server.wm.MiuiFreeFormManagerService");

        findAndHookMethod(mMiuiFreeFormStackDisplayStrategy,"getMaxMiuiFreeFormStackCount", String.class, mMiuiFreeFormActivityStack, XC_MethodReplacement.returnConstant(100));

        findAndHookMethod(mMiuiFreeFormStackDisplayStrategy,"getMaxMiuiFreeFormStackCountForFlashBack", String.class, boolean.class, XC_MethodReplacement.returnConstant(100));

        findAndHookMethod(mMiuiFreeFormManagerService, "shouldStopStartFreeform", String.class, XC_MethodReplacement.returnConstant(false));
    }
}
