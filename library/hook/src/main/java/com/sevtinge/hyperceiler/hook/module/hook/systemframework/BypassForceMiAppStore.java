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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.systemframework;


import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 绕过打开应用商店时强制使用小米应用商店
 *
 * @author LuoYunXi0407
 */
public class BypassForceMiAppStore extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {

        try {
            Class<?> cls = XposedHelpers.findClass(
                "com.android.server.wm.ActivityTaskManagerService", lpparam.classLoader);

            for (Method method : cls.getDeclaredMethods()) {
                if (!method.getName().equals("startActivity")) continue;
                if (!Modifier.isPublic(method.getModifiers())) continue;

                Class<?>[] paramTypes = method.getParameterTypes();
                int intentIndex = -1;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (Intent.class.equals(paramTypes[i])) {
                        intentIndex = i;
                        break;
                    }
                }

                if (intentIndex == -1) continue;

                final int index = intentIndex;
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            Intent intent = (Intent) param.args[index];
                            if (intent == null) return;

                            Uri data = intent.getData();
                            if (!Intent.ACTION_VIEW.equals(intent.getAction()) || data == null)
                                return;


                            String uriStr = data.toString();

                            if (uriStr != null) {
                                // 始终替换 mimarket:// 为 market://
                                uriStr = uriStr.replaceFirst("^mimarket://", "market://");

                                // 如果启用了 detailmini 选项，则替换 path 中的 details → details/detailmini
                                if (mPrefsMap.getBoolean("system_framework_market_use_detailmini")) {
                                    uriStr = uriStr.replaceFirst("(?<=market://)(details)(?!/detailmini)", "details/detailmini");
                                }

                                intent.setData(Uri.parse(uriStr));
                            }

                            // 如果是 market://details?id=...
                            if ("market".equals(intent.getData().getScheme())
                                && ("details".equals(intent.getData().getHost()) || "details/detailmini".equals(intent.getData().getHost()))
                                && intent.getData().getQueryParameter("id") != null
                            ) {

                                // 移除指定包名
                                if (intent.getPackage() != null) {
                                    logI(TAG, "android", "Removed package=：" + intent.getPackage());
                                    intent.setPackage(null);
                                }

                                //FLAG_ACTIVITY_RESET_TASK_IF_NEEDED 会导致小米应用商店无法打开，原因未知
                                intent.removeFlags(intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


                                // 强制 chooser
                                intent = Intent.createChooser(intent, null);



                                logI(TAG, "android", "Forced chooser for market://details intent");
                            }
                            param.args[index] = intent;

                        } catch (Throwable t) {
                            logE(TAG, "android", "Error - " + Log.getStackTraceString(t));

                        }
                    }
                });
                logI(TAG, "android", "Hooked method: " + method);
                break;
            }

        } catch (Throwable t) {
            logE(TAG, "android", "Failed to hook -  " + Log.getStackTraceString(t));
        }
    }


}
