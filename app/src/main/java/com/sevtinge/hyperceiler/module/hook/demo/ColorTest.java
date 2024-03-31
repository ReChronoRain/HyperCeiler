package com.sevtinge.hyperceiler.module.hook.demo;

import android.graphics.Color;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class ColorTest extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.hchen.demo.MainActivity", "setColor",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        mResHook.setObjectReplacement("com.hchen.demo", "color", "my_test_color", Color.RED);
                    }
                }
        );
    }
}
