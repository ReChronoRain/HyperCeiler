package com.sevtinge.hyperceiler.module.hook.home.recent;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class BackgroundBlur extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.common.BlurUtils", "isUseCompleteRecentsBlurAnimation", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.miui.home.launcher.common.BlurUtils", "isUseNoRecentsBlurAnimation", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
