package com.sevtinge.hyperceiler.libhook.rules.photopicker;

import android.content.ComponentName;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class DisableReroute extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.photopicker.hyper.HyperMainActivity", "getHyperFilePickerName", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(new ComponentName(
                    "com.google.android.documentsui",
                    "com.android.documentsui.picker.PickActivity"
                ));
            }
        });
    }
}
