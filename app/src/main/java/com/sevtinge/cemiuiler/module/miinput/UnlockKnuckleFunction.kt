package com.sevtinge.cemiuiler.module.miinput;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import android.content.Context;

public class UnlockKnuckleFunction extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.MiuiShortcut$System", "hasKnockFeature", Context.class, new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
