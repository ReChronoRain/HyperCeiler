package com.sevtinge.hyperceiler.module.hook.milink;

import android.os.Environment;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.io.File;
import java.io.FileNotFoundException;

import de.robv.android.xposed.XC_MethodHook;

public class FuckHpplay extends BaseHook {
    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_PATH = new File(Environment.getExternalStorageDirectory(), "com.milink.service").getAbsolutePath();

    @Override
    public void init() {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        logI(TAG, this.lpparam.packageName, "Target path = " + TARGET_PATH);
        findAndHookMethod("com.hpplay.common.utils.ContextPath", lpparam.classLoader, "makeDir", String[].class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                final boolean isExternalStorage = ((String) param.args[1]).startsWith(TARGET_PATH);
                if (isExternalStorage) {
                    logI(TAG, FuckHpplay.this.lpparam.packageName, "blocked");
                    param.setThrowable(new FileNotFoundException("blocked"));
                }
            }
        });
    }
}
