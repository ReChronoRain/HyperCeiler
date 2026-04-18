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


import android.util.Log;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * 绕过链式跳转检测器
 *
 * @author LuoYunXi0407
 */
public class BypassWakePathChecker extends BaseHook {

    @Override
    public void init() {

        try {
            Class<?> clazz = findClass(
                "com.miui.server.WakePathChecker"
            );
            if (clazz == null) {
                XposedLog.w(TAG, getPackageName(), "Class not found: com.miui.server.WakePathChecker");
                return;
            }

            findAndHookMethod(
                clazz,
                "isAllowedByWakePathRule",
                String.class,
                String.class,
                String.class,
                String.class,
                int.class,
                int.class,
                int.class,
                int.class,
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(true);
                    }
                }
            );

            findAndHookMethod(
                clazz,
                "checkAllowStartActivity",
                String.class,
                String.class,
                int.class,
                int.class,
                android.content.Intent.class,
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(true);
                    }
                }
            );

        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "Failed to hook -  " + Log.getStackTraceString(t));
        }
    }







}
