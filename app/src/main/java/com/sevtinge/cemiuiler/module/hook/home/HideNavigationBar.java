package com.sevtinge.cemiuiler.module.hook.home;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class HideNavigationBar extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.views.RecentsContainer",
            "showLandscapeOverviewGestureView",
            boolean.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.args[0] = false;
                }
            }
        );
    }
}
