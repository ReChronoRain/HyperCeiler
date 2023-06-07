package com.sevtinge.cemiuiler.module.mirror;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class UnlockMiuiPlus extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.xiaomi.mirror.utils.SystemUtils", "isModelSupport", Context.class, new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}





