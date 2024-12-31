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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.utils.shell.ShellUtils;

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
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return Boolean.TRUE.equals(invokeMethod(cls, "getBoolean", new Class[]{String.class, boolean.class}, name, def));
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp int", e);
            return false;
        }
    }

    public static int getProp(String name, int def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getInt", new Class[]{String.class, int.class}, name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp int", e);
            return 0;
        }
    }

    public static long getProp(String name, long def) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            return invokeMethod(cls, "getLong", new Class[]{String.class, long.class}, name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp long", e);
            return 0L;
        }
    }

    public static String getProp(String name, String def) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                    "get", new Class[]{String.class, String.class}, name, def);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp String", e);
            return "";
        }
    }

    public static String getProp(String name) {
        try {
            return invokeMethod(Class.forName("android.os.SystemProperties"),
                    "get", new Class[]{String.class}, name);
        } catch (Throwable e) {
            Log.e(TAG, "PropUtils getProp String no def", e);
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

    private static <T> T invokeMethod(Class<?> cls, String str, Class<?>[] clsArr, Object... objArr) throws Throwable {
        return InvokeUtils.callStaticMethod(cls, str, clsArr, objArr);
    }
}
