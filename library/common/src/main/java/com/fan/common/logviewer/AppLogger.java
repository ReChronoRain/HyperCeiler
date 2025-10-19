package com.fan.common.logviewer;

// AppLogger.java
public class AppLogger extends BaseLogger {

    private static AppLogger sInstance;

    private AppLogger() {
        super("App");
    }

    public static synchronized AppLogger getInstance() {
        if (sInstance == null) {
            sInstance = new AppLogger();
        }
        return sInstance;
    }

    public static void v(String tag, String message) {
        getInstance().log("V", tag, message, true);
    }

    public static void d(String tag, String message) {
        getInstance().log("D", tag, message, true);
    }

    public static void i(String tag, String message) {
        getInstance().log("I", tag, message, true);
    }

    public static void w(String tag, String message) {
        getInstance().log("W", tag, message, true);
    }

    public static void e(String tag, String message) {
        getInstance().log("E", tag, message, true);
    }

    public static void e(String tag, String message, Throwable th) {
        getInstance().log("E", tag, message + th, true);
    }

    public void log(String level, String tag, String message, boolean newLine) {
        LogEntry logEntry = new LogEntry(level, mModuleName, message, tag, newLine);
        LogManager.getInstance().addLog(logEntry);

        // 同时输出到Android Logcat
        switch (level) {
            case "V": android.util.Log.v(mModuleName, message); break;
            case "D": android.util.Log.d(mModuleName, message); break;
            case "I": android.util.Log.i(mModuleName, message); break;
            case "W": android.util.Log.w(mModuleName, message); break;
            case "E": android.util.Log.e(mModuleName, message); break;
        }
    }

    @Override
    public void log(String level, String message, boolean newLine) {

    }
}
