package com.sevtinge.hyperceiler.module.hook.milink;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class UnlockMiShare extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.miui.circulate.world.auth.AuthUtil", "doPermissionCheck", new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) {
                param.setResult(null);
            }
        });
    }
}

