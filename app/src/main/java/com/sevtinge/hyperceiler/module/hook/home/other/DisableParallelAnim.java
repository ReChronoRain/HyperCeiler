/*
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.home.anim;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class HookStateManager extends BaseHook {
// LvguiguiQwQ
    @Override
    public void init() {
        findAndHookMethod("com.miui.home.recents.anim.StateManager",
            "shouldCancelSurfaceAndView",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(true);
                }
            }
        );
    }
}
