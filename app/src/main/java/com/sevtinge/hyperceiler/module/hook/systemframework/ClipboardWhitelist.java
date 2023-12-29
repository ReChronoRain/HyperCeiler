package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ClipboardWhitelist extends BaseHook {
    String[] whitelist;
    @Override
    public void init() {
        Class<?> clipboardClass = XposedHelpers.findClass("com.android.server.clipboard.ClipboardService", lpparam.classLoader);
        String whitelistStr = mPrefsMap.getString("system_framework_clipboard_whitelist_list",
            "com.fooview.android.fooview");
        whitelist = whitelistStr.split("\\|");
        XposedBridge.log("clipboard whitelist");
        hookAllMethods(clipboardClass, "clipboardAccessAllowed", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                for (String pkgName : whitelist) {
                    logE(pkgName);
                    if (pkgName.equals(param.args[1])) {
                        param.setResult(true);
                        return;
                    }
                }
            }
        });
    }
}
