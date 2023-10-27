package com.sevtinge.hyperceiler.module.hook.home.title;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HiddenAllTitle extends BaseHook {
    @Override
    public void init() {
        /*用于隐藏应用名*/
        findAndHookMethod("com.miui.home.launcher.ItemIcon", "setTitle",
            CharSequence.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );
    }
}
