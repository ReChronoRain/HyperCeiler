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
import android.net.Uri;
import android.util.Log;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;
import io.github.libxposed.api.XposedInterface;

/**
 * 绕过打开应用商店时强制使用小米应用商店
 *
 * @author LuoYunXi0407
 */
public class BypassForceMiAppStore extends BaseHook {

    private static final String PREF_FORCE_CHOOSER = "system_framework_bypass_force_mi_appstore";
    private static final String PREF_DETAIL_MINI = "system_framework_market_use_detailmini";

    @Override
    public void init() {

        try {
            Class<?> atmsClass = findClassIfExists("com.android.server.wm.ActivityTaskManagerService");
            if (atmsClass == null) {
                XposedLog.w(TAG, getPackageName(), "Class not found: com.android.server.wm.ActivityTaskManagerService");
                return;
            }

            List<XposedInterface.HookHandle> handles = hookAllMethods(atmsClass, "startActivity", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    try {
                        Object[] args = param.getArgs();
                        int intentIndex = findIntentArgIndex(args);
                        if (intentIndex < 0) return;

                        Intent intent = (Intent) args[intentIndex];
                        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;

                        final boolean forceChooserEnabled = PrefsBridge.getBoolean(PREF_FORCE_CHOOSER);
                        final boolean detailMiniEnabled = PrefsBridge.getBoolean(PREF_DETAIL_MINI);
                        if (!forceChooserEnabled && !detailMiniEnabled) return;

                        Uri data = intent.getData();
                        if (data == null) return;

                        Uri newData = data;
                        String type = intent.getType();

                        // 注意：该 hook 属于系统层 startActivity 拦截，开关间存在联动影响，必须将改写范围收窄到 market 相关 URI。
                        if ("mimarket".equals(newData.getScheme())) {
                            newData = newData.buildUpon().scheme("market").build();
                        }

                        // 如果启用了 detailmini，则将 market://details?id=... 改写为 market://details/detailmini?id=...
                        String path = newData.getPath();
                        if (detailMiniEnabled
                            && isMarketDetailsHost(newData)
                            && (path == null || path.isEmpty() || "/".equals(path))) {
                            newData = newData.buildUpon().path("/detailmini").build();
                        }

                        // setData(...) 会清空 type，使用 setDataAndType(...) 保留 MIME type。
                        if (!newData.equals(data)) {
                            if (type != null) {
                                intent.setDataAndType(newData, type);
                            } else {
                                intent.setData(newData);
                            }
                        }

                        Uri finalData = intent.getData();
                        if (finalData == null) return;

                        // 只处理 market://details[(/detailmini)]?id=...，避免影响其他 ACTION_VIEW intent。
                        if (isMarketDetailsIntent(finalData) && finalData.getQueryParameter("id") != null) {
                            // 移除指定包名
                            if (intent.getPackage() != null) {
                                XposedLog.d(TAG, getPackageName(), "Removed package=：" + intent.getPackage());
                                intent.setPackage(null);
                            }

                            // FLAG_ACTIVITY_RESET_TASK_IF_NEEDED 会导致小米应用商店无法打开，原因未知
                            intent.removeFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                            // 如果启用了 bypass_force_mi_appstore，则强制使用 chooser 打开应用商店
                            if (forceChooserEnabled) {
                                intent = Intent.createChooser(intent, null);
                                XposedLog.d(TAG, getPackageName(), "Forced chooser for market://details intent");
                            }
                        }

                        args[intentIndex] = intent;
                    } catch (Throwable t) {
                        XposedLog.w(TAG, getPackageName(), "Error - " + Log.getStackTraceString(t));
                    }
                }
            });
            if (handles == null || handles.isEmpty()) {
                XposedLog.w(TAG, getPackageName(), "No startActivity overload hooked");
            } else {
                XposedLog.d(TAG, getPackageName(), "Hooked startActivity overloads: " + handles.size());
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

    private static boolean isMarketDetailsHost(Uri uri) {
        return uri != null
            && "market".equals(uri.getScheme())
            && "details".equals(uri.getHost());
    }

    private static boolean isMarketDetailsIntent(Uri uri) {
        if (!isMarketDetailsHost(uri)) return false;

        String path = uri.getPath();
        return path == null || path.isEmpty() || "/".equals(path) || "/detailmini".equals(path);
    }

}
