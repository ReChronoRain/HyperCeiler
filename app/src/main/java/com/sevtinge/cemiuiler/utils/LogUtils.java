package com.sevtinge.cemiuiler.utils;

import android.util.Log;

import de.robv.android.xposed.XposedBridge;
import com.sevtinge.cemiuiler.BuildConfig;

public class LogUtils {

    public static final String TAG = "Cemiuiler";
    public static final String mStartWith = "Cemiuiler: ";
    public static boolean isDebugVersion = BuildConfig.DEBUG;

    public static void log (String message) {
        String mLog = mStartWith + message;
        XposedBridge.log(mLog);
    }

    public static void log (Throwable tr) {
        String mErro = mStartWith + "Erro: [" + tr.getMessage() +  "]";
        XposedBridge.log(mErro);
    }

    public static void log (String message, Throwable tr) {
        log(message);
        log(tr);
    }

    public static void logXp(String mod, String message) {
        log(mod + " " + message);
    }

    public static void logXp(String mod, Throwable tr) {
        log(mod + " " + tr.getMessage());
    }

    public static void logXp(String mod, String message, Throwable tr) {
        logXp(mod, message);
        log(tr);
    }


    public static void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        Log.d(tag, msg, tr);
    }


    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        Log.i(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }


}
