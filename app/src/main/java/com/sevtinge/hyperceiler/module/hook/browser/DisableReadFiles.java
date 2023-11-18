package com.sevtinge.hyperceiler.module.hook.browser;

import android.net.Uri;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableReadFiles extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.browser.provider.AdBlockRuleProvider", "openFile", Uri.class, String.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
