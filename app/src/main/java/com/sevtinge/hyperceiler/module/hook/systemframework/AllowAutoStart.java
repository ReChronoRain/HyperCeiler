package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import com.hchen.hooktool.callback.IAction;
import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.module.base.BaseTool;
import com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver;

import java.util.HashSet;
import java.util.Set;

public class AllowAutoStart extends BaseTool {
    private Set<String> strings = new HashSet<>();
    private ApplicationInfo calleeInfo = null;

    @Override
    public void doHook() {
        classTool.findClass("ash", "miui.app.ActivitySecurityHelper")
                .getConstructor(Context.class)
                .hook(new IAction() {
                    @Override
                    public void after(ParamTool param) {
                        Context context = param.first();
                        new PrefsChangeObserver(context, new Handler(context.getMainLooper()), true,
                                "prefs_key_system_framework_auto_start_apps");
                    }
                })
                .getMethod("getCheckStartActivityIntent", ApplicationInfo.class, ApplicationInfo.class,
                        Intent.class, boolean.class, int.class, boolean.class, int.class, int.class)
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        calleeInfo = param.second();
                    }
                })
                .getMethod("restrictForChain", ApplicationInfo.class)
                .hook(new IAction() {
                    @Override
                    public void before(ParamTool param) {
                        strings = mPrefsMap.getStringSet("system_framework_auto_start_apps");
                        ApplicationInfo info = param.first();
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
