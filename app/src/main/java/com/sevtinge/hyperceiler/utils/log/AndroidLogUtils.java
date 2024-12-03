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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils.log;

import android.util.Log;

import com.sevtinge.hyperceiler.utils.PropUtils;

/* 不太建议在非 Xposed 代码使用处调用，虽然已经做了 try 处理，但是 detailLog 将始终为 false
 * 可能因为 <BaseHook.mPrefsMap.getBoolean("settings_disable_detailed_log");>
 * 会导致 <java.lang.NoClassDefFoundError: Failed resolution of: Lcom/sevtinge/hyperceiler/XposedInit;> 等
 * 日记:
 * 2024/1/3
 * 我的评价是不要限制这个。
 * 2024/1/4
 * 我爱 prop。
 * */
public class AndroidLogUtils {
    private static final String Tag = "[HyperCeiler]: ";
    private static final int logLevel = PropUtils.getProp("persist.hyperceiler.log.level", 3);

    public static void logI(String tag, String msg) {
        if (logLevel < 3) return;
        Log.i(tag, "[I]" + Tag + msg);
    }

    public static void deLogI(String tag, String msg) {
        Log.i(tag, "[I/" + Tag + msg);
    }

    public static void logD(String tag, Throwable tr) {
        if (logLevel < 4) return;
        Log.d(tag, "[D]" + Tag, tr);
    }

    public static void logD(String tag, String msg, Throwable tr) {
        if (logLevel < 4) return;
        Log.d(tag, "[D]" + Tag + msg, tr);
    }

    public static void logW(String tag, String msg, Throwable tr) {
        if (logLevel < 2) return;
        Log.w(tag, "[W]" + Tag + msg, tr);
    }

    public static void logW(String tag, String msg) {
        if (logLevel < 2) return;
        Log.w(tag, "[W]" + Tag + msg, null);
    }

    public static void logE(String tag, Throwable tr) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag, tr);
    }

    public static void logE(String tag, String msg) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag + msg, null);
    }

    public static void logE(String tag, String msg, Throwable tr) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag + msg, tr);
    }
}
