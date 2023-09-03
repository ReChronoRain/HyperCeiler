package com.sevtinge.cemiuiler.module.hook.systemframework;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class FreeformBubble extends BaseHook {

    Class<?> mMiuiMultiWindowUtils;

    @Override
    public void init() {

        mMiuiMultiWindowUtils = findClassIfExists("android.util.MiuiMultiWindowUtils");

        findAndHookMethod(mMiuiMultiWindowUtils, "multiFreeFormSupported", Context.class, XC_MethodReplacement.returnConstant(true));
    }
}
