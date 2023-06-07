package com.sevtinge.cemiuiler.module.milink;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class UnlockMiShare extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.miui.circulate.world.auth.AuthUtil", "doPermissionCheck", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
        hookAllMethods("com.miui.circulate.world.utils.GetKeyUtil", "doWhiteListAuth", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}

