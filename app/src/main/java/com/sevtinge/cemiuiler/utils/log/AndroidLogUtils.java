package com.sevtinge.cemiuiler.utils.log;

import android.util.Log;

import com.sevtinge.cemiuiler.BuildConfig;

import de.robv.android.xposed.XposedBridge;

public class AndroidLogUtils {
    public static final String Tag = "Cemiuiler]: ";
    public static boolean isDebugVersion = !BuildConfig.BUILD_TYPE.contains("release");


    public static void LogD(String tag, String msg) {
        if (!isDebugVersion) return;
        Log.d(tag, "[D/" + Tag + msg);
    }

    public static void LogD(String tag, String msg, Throwable tr) {
        if (!isDebugVersion) return;
        Log.d(tag, "[D/" + Tag + msg, tr);
    }


    public static void LogI(String tag, String msg) {
        if (!isDebugVersion) return;
        Log.i(tag, "[I/" + Tag + msg);
    }

    public static void LogI(String tag, String msg, Throwable tr) {
        if (!isDebugVersion) return;
        Log.i(tag, "[I/" + Tag + msg, tr);
    }

    public static void LogE(String tag, String msg) {
        if (!isDebugVersion) return;
        Log.e(tag, "[E/" + Tag + msg);
    }

    public static void LogE(String tag, String msg, Throwable tr) {
        if (!isDebugVersion) return;
        Log.e(tag, "[E/" + Tag + msg, tr);
    }
}
