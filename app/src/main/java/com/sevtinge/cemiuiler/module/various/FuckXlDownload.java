package com.sevtinge.cemiuiler.module.various;

import android.os.Environment;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.io.File;
import java.io.FileNotFoundException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class FuckXlDownload extends BaseHook {
    private static final String TAG = "XlDownload";
    private static final String TARGET_PACKAGE = "com.android.providers.downloads";
    private static final File TARGET_PATH = new File(Environment.getExternalStorageDirectory(), ".xlDownload").getAbsoluteFile();

    @Override
    public void init() {
        log("Cemiuiler: " + TAG + "Injected to process = " + lpparam.packageName);

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("Cemiuiler: " + TAG + "Target path = " + TARGET_PATH);

        XposedHelpers.findAndHookMethod(File.class, "mkdirs", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                log("Cemiuiler: " + TAG + "path = " + param.thisObject);

                final boolean isXlDownload = ((File) param.thisObject).getAbsoluteFile().equals(TARGET_PATH);
                if (isXlDownload) {
                    log(TAG + "blocked");
                    param.setThrowable(new FileNotFoundException("blocked"));
                }
            }
        });
    }
}
