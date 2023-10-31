package com.sevtinge.hyperceiler.utils.log;

import static com.sevtinge.hyperceiler.utils.BuildUtils.getBuildType;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.PrefsMap;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class XposedLogUtils {

    public XC_LoadPackage.LoadPackageParam mLoadPackageParam = null;

    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;
    }

    public static final PrefsMap<String, Object> mPrefsMap = XposedInit.mPrefsMap;
    public static final boolean isDebugVersion = getBuildType().equals("debug");
    public static final boolean isNotReleaseVersion = !getBuildType().equals("release");
    public static final boolean isReleaseVersion = getBuildType().equals("release");
    public final boolean detailLog = !mPrefsMap.getBoolean("settings_disable_detailed_log");
    public static final boolean isDisableDetailLog = mPrefsMap.getBoolean("settings_disable_detailed_log");

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

    public static void logI(String tag, String pkg, String msg) {
        if (!isDebugVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][I][" + pkg + "][" + tag + "]: " + msg);
    }

    public static void logW(String msg) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W]: " + msg);
    }

    public static void logW(String tag, String pkg, String msg) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + msg);
    }

    public static void logW(String tag, String pkg, Throwable log) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + log);
    }

    public static void logW(String tag, String pkg, String msg, Exception exp) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + msg + ", by: " + exp);
    }

    public static void logW(String tag, String pkg, String msg, Throwable log) {
        if (isReleaseVersion) return;
        if (isDisableDetailLog) return;
        XposedBridge.log("[HyperCeiler][W][" + pkg + "][" + tag + "]: " + msg + ", by: " + log);
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
        XposedBridge.log("[HyperCeiler][W][" + tag + "]: " + msg + ", by: " + exp);
    }

    public static void logE(String tag, String msg) {
        XposedBridge.log("[HyperCeiler][E][" + tag + "]: " + msg);
    }

    public static void logE(String msg) {
        XposedBridge.log("[HyperCeiler][E]: " + msg);
    }

    public static void logE(String tag, Throwable log) {
        XposedBridge.log("[HyperCeiler][E][" + tag + "]: " + log);
    }

    public static void logE(String tag, String pkg, String msg) {
        XposedBridge.log("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + msg);
    }

    public static void logE(String tag, String pkg, Throwable log) {
        XposedBridge.log("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + log);
    }

    public static void logE(String tag, String pkg, Exception exp) {
        XposedBridge.log("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + exp);
    }

    public static void logE(String tag, String pkg, String msg, Throwable log) {
        XposedBridge.log("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + msg + ", by: " + log);
    }

    public static void logE(String tag, String pkg, String msg, Exception exp) {
        XposedBridge.log("[HyperCeiler][E][" + pkg + "][" + tag + "]: " + msg + ", by: " + exp);
    }

    public static void logD(String msg) {
        if (!isDebugVersion) return;
        XposedBridge.log("[HyperCeiler][D]: " + msg);
    }

    public static void logD(String tag, String pkg, String msg) {
        if (!isDebugVersion) return;
        XposedBridge.log("[HyperCeiler][D][" + pkg + "][" + tag + "]: " + msg);
    }

}
