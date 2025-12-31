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
package com.sevtinge.hyperceiler.hook.utils;

import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.checkRootPermission;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import com.sevtinge.hyperceiler.hook.callback.ITAG;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils;

@SuppressLint("PrivateApi")
public class PropUtils {
    private static final String TAG = ITAG.TAG;

    public static String getProp(Context context, String name) {
        try {
            return classLoaderMethod(context, name);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils classLoader getProp String", e);
            return "";
        }
    }

    public static boolean getProp(String name, boolean def) {
        try {
            return SystemProperties.getBoolean(name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp int", e);
            return false;
        }
    }

    public static int getProp(String name, int def) {
        try {
            return SystemProperties.getInt(name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp int", e);
            return 0;
        }
    }

    public static long getProp(String name, long def) {
        try {
            return SystemProperties.getLong(name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp long", e);
            return 0L;
        }
    }

    public static String getProp(String key, String defaultValue) {
        try {
            return SystemProperties.get(key, defaultValue);
        } catch (Throwable throwable) {
            Log.e("getProp", "key get e: " + key + " will return default: " + defaultValue + " e:" + throwable);
            return defaultValue;
        }
    }

    public static String getProp(String name) {
        try {
            return SystemProperties.get(name);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp String no def", e);
            return "";
        }
    }

    public static String getPropSu(String name) {
        try {
            if (checkRootPermission() == 0) {
                return ShellUtils.rootExecCmd("getprop " + name);
            } else {
                return "";
            }
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getPropSu String no def", e);
            return "";
        }
    }

    /**
     * 系统限制只能使用Root。
     * 返回 true 表示成功。
     *
     * @return boolean
     */
    public static boolean setProp(String name, Object vale) {
        if (ShellInit.getShell() != null) {
            return ShellInit.getShell().run("setprop " + name + " " + vale).sync().isResult();
        }
        return ShellUtils.getResultBoolean("setprop " + name + " " + vale, true);
    }

    private static String classLoaderMethod(Context context, String name) throws Throwable {
        ClassLoader classLoader = context.getClassLoader();
        return InvokeUtils.callStaticMethod("android.os.SystemProperties", classLoader,
                "get", new Class[]{String.class}, name);
    }
}
