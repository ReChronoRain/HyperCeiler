/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Intent;
import android.util.Log;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;
import io.github.libxposed.api.XposedInterface;

/**
 * 修复从快速分享查看下载的内容时跳转错误
 *
 * @author LuoYunXi0407
 */
public class BypassForceDownloadui extends BaseHook {

    private static final String ACTION_VIEW_DOWNLOADS = "android.intent.action.VIEW_DOWNLOADS";

    @Override
    public void init() {

        try {
            Class<?> ascClass = findClassIfExists("com.android.server.wm.ActivityStartController");
            if (ascClass == null) {
                XposedLog.w(TAG, getPackageName(), "Class not found: com.android.server.wm.ActivityStartController");
                return;
            }

            List<XposedInterface.HookHandle> handles = hookAllMethods(ascClass, "startActivityInPackage", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    try {
                        Object[] args = param.getArgs();
                        int intentIndex = findIntentArgIndex(args);
                        if (intentIndex < 0) return;

                        Intent intent = (Intent) args[intentIndex];
                        if (intent == null) return;

                        // 注意：该 hook 同样处于系统层 startActivity 拦截链路，范围较大，必须严格限制到 VIEW_DOWNLOADS 场景。
                        // 这样可以降低与应用商店相关 hook 在同一调用链上互相影响的概率。
                        if (Intent.ACTION_CHOOSER.equals(intent.getAction())) return;
                        if (!ACTION_VIEW_DOWNLOADS.equals(intent.getAction())) return;

                        intent.setPackage(null); // 移除指定包名，如果不移除 documentsui 也会强制跳到 downloads.ui
                        args[intentIndex] = Intent.createChooser(intent, null);
                        XposedLog.d(TAG, getPackageName(), "Forced chooser for android.intent.action.VIEW_DOWNLOADS");
                    } catch (Throwable t) {
                        XposedLog.w(TAG, getPackageName(), "Error - " + Log.getStackTraceString(t));
                    }
                }
            });
            if (handles == null || handles.isEmpty()) {
                XposedLog.w(TAG, getPackageName(), "No startActivityInPackage overload hooked");
            } else {
                XposedLog.d(TAG, getPackageName(), "Hooked startActivityInPackage overloads: " + handles.size());
            }

        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "Failed to hook -  " + Log.getStackTraceString(t));

        }

    }

    private static int findIntentArgIndex(Object[] args) {
        if (args == null) return -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Intent) return i;
        }
        return -1;
    }
}
