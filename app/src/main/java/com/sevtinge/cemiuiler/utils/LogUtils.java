package com.sevtinge.cemiuiler.utils;

import android.util.Log;

import com.sevtinge.cemiuiler.BuildConfig;

import de.robv.android.xposed.XposedBridge;

public class LogUtils {

    public static final String TAG = "Cemiuiler";
    public static final String mStartWith = "[Cemiuiler] ";
    public static boolean isDebugVersion = !BuildConfig.BUILD_TYPE.contains("release");

    public static void log(String message) {
        if (!isDebugVersion)
            return;

        String mLog = mStartWith + message;
        XposedBridge.log(mLog);
    }

    public static void log(Throwable tr) {
        if (!isDebugVersion)
            return;

        String mErro = mStartWith + "Erro: [" + tr.getMessage() + "]";
        XposedBridge.log(mErro);
    }

    public static void log(String message, Throwable tr) {
        if (!isDebugVersion)
            return;

        log(message);
        log(tr);
    }

    public static void logXp(String mod, String message) {
        if (!isDebugVersion)
            return;

        log(mod + " " + message);
    }

    public static void logXp(String mod, Throwable tr) {
        if (!isDebugVersion)
            return;

        log(mod + " " + tr.getMessage());
    }

    public static void logXp(String mod, String message, Throwable tr) {
        if (!isDebugVersion)
            return;

        logXp(mod, message);
        log(tr);
    }


    public static void d(String tag, String msg) {
        if (!isDebugVersion)
            return;

        Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (!isDebugVersion)
            return;

        Log.d(tag, msg, tr);
    }


    public static void i(String tag, String msg) {
        if (!isDebugVersion)
            return;

        Log.i(tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (!isDebugVersion)
            return;

        Log.i(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        if (!isDebugVersion)
            return;

        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (!isDebugVersion)
            return;

        Log.e(tag, msg, tr);
    }


}
