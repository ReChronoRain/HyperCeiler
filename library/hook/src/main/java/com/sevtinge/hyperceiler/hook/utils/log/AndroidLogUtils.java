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
package com.sevtinge.hyperceiler.hook.utils.log;

import android.util.Log;

/*
 * 请在非 Xposed 代码下使用此类进行日志记录
 *
 */
public class AndroidLogUtils {
    private static final String Tag = "HyperCeiler: ";
    private static final int logLevel;

    /**
     * 日志监听器接口，用于将日志转发到 LogViewer
     */
    public interface LogListener {
        void onLog(String level, String tag, String message);
    }

    private static LogListener sLogListener;

    /**
     * 设置日志监听器（在 Application 中调用）
     */
    public static void setLogListener(LogListener listener) {
        sLogListener = listener;
    }

    private static void notifyListener(String level, String tag, String message) {
        if (sLogListener != null) {
            try {
                sLogListener.onLog(level, tag, message);
            } catch (Throwable ignored) {
                // 忽略监听器异常，避免影响正常日志输出
            }
        }
    }

    static {
        int level = 3;
        try {
            level = LogManager.readLogLevelFromFile(null);
        } catch (Throwable t) {
            Log.e("HyperCeiler:AndroidLogUtils", "Failed to get log level from LogManager", t);
        }
        logLevel = level;
    }


    public static void logI(String tag, String msg) {
        if (logLevel < 3) return;
        Log.i(tag, "[I]" + Tag + msg);
        notifyListener("I", tag, msg);
    }

    public static void deLogI(String tag, String msg) {
        Log.i(tag, "[I/" + Tag + msg);
        notifyListener("I", tag, msg);
    }

    public static void logD(String tag, Throwable tr) {
        if (logLevel < 4) return;
        Log.d(tag, "[D]" + Tag, tr);
        notifyListener("D", tag, tr != null ? tr.toString() : "");
    }

    public static void logD(String tag, String msg, Throwable tr) {
        if (logLevel < 4) return;
        Log.d(tag, "[D]" + Tag + msg, tr);
        notifyListener("D", tag, msg + (tr != null ? "\n" + tr : ""));
    }

    public static void logW(String tag, String msg, Throwable tr) {
        if (logLevel < 2) return;
        Log.w(tag, "[W]" + Tag + msg, tr);
        notifyListener("W", tag, msg + (tr != null ? "\n" + tr : ""));
    }

    public static void logW(String tag, String msg) {
        if (logLevel < 2) return;
        Log.w(tag, "[W]" + Tag + msg, null);
        notifyListener("W", tag, msg);
    }

    public static void logE(String tag, Throwable tr) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag, tr);
        notifyListener("E", tag, tr != null ? tr.toString() : "");
    }

    public static void logE(String tag, String msg) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag + msg, null);
        notifyListener("E", tag, msg);
    }

    public static void logE(String tag, String msg, Throwable tr) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag + msg, tr);
        notifyListener("E", tag, msg + (tr != null ? "\n" + tr : ""));
    }
}
