/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.prefs.PrefsChangeObserver;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.HashSet;
import java.util.Set;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AllowAutoStart extends BaseHook {
    private Set<String> strings = new HashSet<>();
    private ApplicationInfo calleeInfo = null;

    @Override
    public void init() {
        findAndHookConstructor("miui.app.ActivitySecurityHelper", Context.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Context context = (Context) param.getArgs()[0];
                new PrefsChangeObserver(context, new Handler(context.getMainLooper()), true,
                    "prefs_key_system_framework_auto_start_apps");
            }
        });

        findAndHookMethod("miui.app.ActivitySecurityHelper", "getCheckStartActivityIntent", ApplicationInfo.class, ApplicationInfo.class, Intent.class, boolean.class, int.class, boolean.class, int.class, int.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                calleeInfo = (ApplicationInfo) param.getArgs()[1];
            }
        });

        findAndHookMethod("miui.app.ActivitySecurityHelper", "restrictForChain", ApplicationInfo.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                strings = PrefsBridge.getStringSet("system_framework_auto_start_apps");
                ApplicationInfo info = (ApplicationInfo) param.getArgs()[0];
                if (calleeInfo != null) {
                    if (strings.contains(calleeInfo.packageName)) {
                        XposedLog.d(TAG, "Boot has been allowed! caller" + info.packageName + " callee: " + calleeInfo.packageName);
                        param.setResult(false);
                    }
                }
            }
        });
    }
}
