package com.sevtinge.hyperceiler.utils.log;

import android.util.Log;

import com.sevtinge.hyperceiler.utils.PropUtils;

/* 不太建议在非 Xposed 代码使用处调用，虽然已经做了 try 处理，但是 detailLog 将始终为 false
 * 可能因为 <BaseHook.mPrefsMap.getBoolean("settings_disable_detailed_log");>
 * 会导致 <java.lang.NoClassDefFoundError: Failed resolution of: Lcom/sevtinge/hyperceiler/XposedInit;> 等
 * 日记:
 * 2024/1/3
 * 我我的评价是不要限制这个。
 * 2024/1/4
 * 我爱prop。
 * */
public class AndroidLogUtils {
    private static final String Tag = "[HyperCeiler]: ";
    private static final int logLevel = PropUtils.getProp("hyperceiler_log_level", 2);

    public static void LogI(String tag, String msg) {
        if (logLevel < 3) return;
        Log.i(tag, "[I]" + Tag + msg);
    }

    public static void deLogI(String tag, String msg) {
        Log.i(tag, "[I/" + Tag + msg);
    }

    public static void LogD(String tag, Throwable tr) {
        if (logLevel < 4) return;
        Log.d(tag, "[D]" + Tag, tr);
    }

    public static void LogD(String tag, String msg, Throwable tr) {
        if (logLevel < 4) return;
        Log.d(tag, "[D]" + Tag + msg, tr);
    }

    public static void LogE(String tag, Throwable tr) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag, tr);
    }

    public static void LogE(String tag, String msg, Throwable tr) {
        if (logLevel < 1) return;
        Log.e(tag, "[E]" + Tag + msg, tr);
    }
}
