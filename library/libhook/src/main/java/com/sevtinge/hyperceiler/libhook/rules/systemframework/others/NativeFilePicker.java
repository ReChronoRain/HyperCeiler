package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Intent;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class NativeFilePicker extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerServiceImpl", "mayReferToFileExplore", Intent.class, String.class, new IMethodHook() {
            @Override
            public void before(HookParam param) throws Throwable {
                param.setResult(param.getArgs()[0]);
            }
        });
    }
}
