package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.sevtinge.hyperceiler.module.base.tool.HookTool.mPrefsMap;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.callback.IAction;
import com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver;

import java.util.HashSet;
import java.util.Set;

public class AllowAutoStart extends BaseHC {
    private Set<String> strings = new HashSet<>();
    private ApplicationInfo calleeInfo = null;

    @Override
    public void init() {
        chain("miui.app.ActivitySecurityHelper", constructor(Context.class)
                .hook(new IAction() {
                    @Override
                    public void after() throws Throwable {
                        Context context = first();
                        new PrefsChangeObserver(context, new Handler(context.getMainLooper()), true,
                                "prefs_key_system_framework_auto_start_apps");
                    }
                })

                .method("getCheckStartActivityIntent", ApplicationInfo.class, ApplicationInfo.class,
                        Intent.class, boolean.class, int.class, boolean.class, int.class, int.class)
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        calleeInfo = second();
                    }
                })

                .method("restrictForChain", ApplicationInfo.class)
                .hook(new IAction() {
                    @Override
                    public void before() throws Throwable {
                        strings = mPrefsMap.getStringSet("system_framework_auto_start_apps");
                        ApplicationInfo info = first();
                        if (calleeInfo != null) {
                            if (strings.contains(calleeInfo.packageName)) {
                                logI(TAG, "Boot has been allowed! caller" + info.packageName + " callee: " + calleeInfo.packageName);
                                returnFalse();
                            }
                        }
                    }
                })
        );
    }
}
