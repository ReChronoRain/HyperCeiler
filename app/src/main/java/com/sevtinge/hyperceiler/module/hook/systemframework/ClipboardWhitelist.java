/*package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Set;

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
                    //logE(pkgName);
                    logD(TAG, lpparam.packageName, "ClipboardWhitelist current app is: " + pkgName);
                    logD(TAG, lpparam.packageName, "ClipboardWhitelist param app is: " + param.args[1]);
                    logD(TAG, lpparam.packageName, "ClipboardWhitelist whitelist apps is: " + whitelist);
                    if (pkgName.equals(param.args[1])) {
                        param.setResult(true);
                        logD(TAG, lpparam.packageName, pkgName + "is in whitelist.");
                        return;
                    }
                }
            }
        });
    }
}*/


package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Set;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ClipboardWhitelist extends BaseHook {
    String[] whitelist;
    @Override
    public void init() {
        Class<?> clipboardClass = XposedHelpers.findClass("com.android.server.clipboard.ClipboardService", lpparam.classLoader);
        String key = "system_framework_clipboard_whitelist_apps";
        Set<String> selectedApps = mPrefsMap.getStringSet(key);
        //String whitelistStr = mPrefsMap.getString("system_framework_clipboard_whitelist_list","com.fooview.android.fooview");
        //whitelist = whitelistStr.split("\\|");
        //XposedBridge.log("clipboard whitelist");
        hookAllMethods(clipboardClass, "clipboardAccessAllowed", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                for (String pkgName : selectedApps) {
                    logD(TAG, lpparam.packageName, "ClipboardWhitelist current app is: " + pkgName);
                    logD(TAG, lpparam.packageName, "ClipboardWhitelist param app is: " + param.args[1]);
                    logD(TAG, lpparam.packageName, "ClipboardWhitelist selectedApps is: " + selectedApps);
                    if (pkgName.equals(param.args[1])) {
                        param.setResult(true);
                        logD(TAG, lpparam.packageName, pkgName + " is in whitelist.");
                        return;
                    }
                }
            }
        });
    }
}


