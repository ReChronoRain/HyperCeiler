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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 修复从快速分享查看下载的内容时跳转错误
 *
 * @author LuoYunXi0407
 */
public class BypassForceDownloadui extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {

        try {
            Class<?> cls = XposedHelpers.findClass(
                "com.android.server.wm.ActivityStartController", lpparam.classLoader);

            for (Method method : cls.getDeclaredMethods()) {
                if (!method.getName().equals("startActivityInPackage")) continue;
                //if (!Modifier.isPublic(method.getModifiers())) continue;

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

                            // Uri data = intent.getData();  // ?

                            if (!"android.intent.action.VIEW_DOWNLOADS".equals(intent.getAction()))
                                return;



                            intent.setPackage(null); // 移除指定包名，如果不移除 documentsui 也会强制跳到 downloads.ui

                            Intent chooser = Intent.createChooser(intent, null);

                            param.args[index] = chooser;

                            logI(TAG, "android", "Forced chooser for android.intent.action.VIEW_DOWNLOADS");



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
