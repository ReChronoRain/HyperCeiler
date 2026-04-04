package com.sevtinge.hyperceiler.libhook.rules.photopicker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class DisableReroute extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.photopicker.hyper.HyperMainActivity", "getHyperFilePickerName", new IMethodHook() {
            @Override
            public void before(HookParam param) throws Throwable {
                Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
                intent.addCategory("android.intent.category.OPENABLE");
                intent.setType("*/*");
                PackageManager packageManager = (PackageManager) callMethod(param.getThisObject(), "getPackageManager");
                ComponentName componentName = intent.resolveActivity(packageManager);
                param.setResult(componentName);
            }
        });
    }
}
