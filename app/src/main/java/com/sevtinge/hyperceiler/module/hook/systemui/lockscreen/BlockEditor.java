package com.sevtinge.hyperceiler.module.hook.systemui.lockscreen;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class BlockEditor extends BaseHook {
    Class<?> mKeyguardEditorHelperCls;

    @Override
    public void init() {

        mKeyguardEditorHelperCls = findClassIfExists("com.android.keyguard.KeyguardEditorHelper");
        findAndHookMethod(mKeyguardEditorHelperCls, "checkIfStartEditActivity", new replaceHookedMethod() {
            @Override
            protected Object replace(MethodHookParam param) throws Throwable {
                return null;
            }
        });
    }
}
