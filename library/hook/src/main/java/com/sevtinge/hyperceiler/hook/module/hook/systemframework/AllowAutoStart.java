package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsChangeObserver;

import java.util.HashSet;
import java.util.Set;

public class AllowAutoStart extends BaseHook {
    private Set<String> strings = new HashSet<>();
    private ApplicationInfo calleeInfo = null;

    @Override
    public void init() {
        findAndHookConstructor("miui.app.ActivitySecurityHelper", Context.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                new PrefsChangeObserver(context, new Handler(context.getMainLooper()), true,
                        "prefs_key_system_framework_auto_start_apps");
            }
        });

        findAndHookMethod("miui.app.ActivitySecurityHelper", "getCheckStartActivityIntent", ApplicationInfo.class, ApplicationInfo.class, Intent.class, boolean.class, int.class, boolean.class, int.class, int.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        calleeInfo = (ApplicationInfo) param.args[1];
                    }
                });

        findAndHookMethod("miui.app.ActivitySecurityHelper", "restrictForChain", ApplicationInfo.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                strings = mPrefsMap.getStringSet("system_framework_auto_start_apps");
                ApplicationInfo info = (ApplicationInfo) param.args[0];
                if (calleeInfo != null) {
                    if (strings.contains(calleeInfo.packageName)) {
                        logI(TAG, "Boot has been allowed! caller" + info.packageName + " callee: " + calleeInfo.packageName);
                        param.setResult(false);
                    }
                }
            }
        });
    }
}
