package com.sevtinge.cemiuiler.module.hook.securitycenter;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class VideoDolbyOpen extends BaseHook {
    @Override
    public void init() {
        try {
            findClassIfExists("com.miui.gamebooster.service.DockWindowManagerService").getDeclaredMethod("N");
            findAndHookMethod("com.miui.gamebooster.service.DockWindowManagerService", "N", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    logI("Hook N");
                    param.setResult(null);
                }
            });
        } catch (NoSuchMethodException e) {
            logI("Don't Find DockWindowManagerService$N");
        }
    }
}
