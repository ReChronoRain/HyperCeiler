package com.sevtinge.hyperceiler.utils.log;

import java.util.Optional;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedBridge;


public class XposedLogUtils {

    private static boolean isDebugVersion = BuildConfig.BUILD_TYPE.contains("debug");
    private static boolean isReleaseVersion = BuildConfig.BUILD_TYPE.contains("release");
    private static boolean isDisableDetailLog = BaseHook.mPrefsMap.getBoolean("settings_disable_detailed_log");

    public static void logI(String msg) {
        if (!isDebugVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][I]: " + msg);
    }

    public static void logI(String tag, String msg) {
        if (!isDebugVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][I][" + tag + "]: " + msg);
    }

    public static void logW(String msg) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W]: " + msg);
    }

    public static void logW(String tag, String msg) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + tag + "]: " + msg);
    }

    public static void logW(String tag, Throwable log) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + tag + "]: " + log);
    }

    public static void logW(String tag, String msg, Exception exp) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + tag + "]: " + msg + ", by" + exp);
    }

    public static void logW(String tag, String msg, Throwable log) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + tag + "]: " + msg + ", by" + log);
    }

    public static void logE(String tag, String msg) {
        XposedBridge.log("[HyperCeiler][E][" + tag + "]: " + msg);
    }

    public static void logE(String tag, Throwable log) {
        XposedBridge.log("[HyperCeiler][E][" + tag + "]: " + log);
    }

    public static void logE(String tag, Exception exp) {
        XposedBridge.log("[HyperCeiler][E][" + tag + "]: " + exp);
    }

    public static void logE(String tag, String msg, Throwable log) {
        XposedBridge.log("[HyperCeiler][E][" + tag + "]: " + msg + ", by" + log);
    }

    public static void logE(String tag, String msg, Exception exp) {
        XposedBridge.log("[HyperCeiler][E][" + tag + "]: " + msg + ", by" + exp);
    }

    public static void logD(String msg) {
        if (!isDebugVersion) return;
        XposedBridge.log("[HyperCeiler][D]: " + msg);
    }

    public static void logD(String tag, String msg) {
        if (!isDebugVersion) return;
        XposedBridge.log("[HyperCeiler][D][" + tag + "]: " + msg);
    }

}
