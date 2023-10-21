package com.sevtinge.cemiuiler.utils.log;

import android.util.Log;

import com.sevtinge.cemiuiler.BuildConfig;
import com.sevtinge.cemiuiler.module.base.BaseHook;

/* 不要在非Xposed代码使用处调用，容易导致闪退
 * 可能因为 <BaseHook.mPrefsMap.getBoolean("settings_disable_detailed_log");>
 * 会导致 <java.lang.NoClassDefFoundError: Failed resolution of: Lcom/sevtinge/cemiuiler/XposedInit;>
 * */
public class AndroidLogUtils {
    private static final String Tag = "Cemiuiler]: ";
    private static final boolean isDebugVersion = BuildConfig.BUILD_TYPE.contains("debug");
    private static final boolean isNotReleaseVersion = !BuildConfig.BUILD_TYPE.contains("release");
    private static final boolean detailLog = BaseHook.mPrefsMap.getBoolean("settings_disable_detailed_log");


    public static void LogI(String tag, String msg) {
        if (!isDebugVersion) return;
        if (detailLog) return;
        Log.i(tag, "[I/" + Tag + msg);
    }

    public static void deLogI(String tag, String msg) {
        Log.i(tag, "[I/" + Tag + msg);
    }

    public static void LogD(String tag, Throwable tr) {
        if (!isDebugVersion) return;
        if (detailLog) return;
        Log.d(tag, "[D/" + Tag, tr);
    }

    public static void LogD(String tag, String msg, Throwable tr) {
        if (!isDebugVersion) return;
        if (detailLog) return;
        Log.d(tag, "[D/" + Tag + msg, tr);
    }

    public static void LogE(String tag, Throwable tr) {
        if (!isDebugVersion) return;
        Log.e(tag, "[E/" + Tag, tr);
    }

    public static void LogE(String tag, String msg, Throwable tr) {
        if (!isDebugVersion) return;
        Log.e(tag, "[E/" + Tag + msg, tr);
    }
}
