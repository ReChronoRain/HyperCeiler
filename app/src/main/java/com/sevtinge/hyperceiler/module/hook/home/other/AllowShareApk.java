package com.sevtinge.hyperceiler.module.hook.home.other;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AllowShareApk extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.common.Utilities", "isSecurityCenterSupportShareAPK", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(false);
                }
            }
        );
    }
}
